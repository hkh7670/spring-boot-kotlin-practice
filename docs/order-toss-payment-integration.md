# 주문 + Toss Payments 연동

작업 일자: 2026-07-19 ~ 2026-07-20
관련 커밋: `3d56566` 주문 로직 구현

## 목표

쇼핑몰(쿠팡류) 백엔드의 주문 생성 → Toss Payments 결제 승인 → 결제 정보 저장 흐름 구현.

## 전체 플로우

```
1. POST /api/v1/orders
   - 재고 검증 + 원자적 차감, order_info(status=****PENDING_PAYMENT) + order_detail_info 생성
   - 응답: orderId, orderUid, productTotalPrice, deliveryPrice, totalPrice

2. (프론트) Toss 결제위젯 requestPayment(orderId=orderUid, amount=totalPrice)
   - 사용자가 결제창에서 결제 완료
   - Toss가 브라우저를 successUrl?paymentKey=...&orderId=...&amount=... 로 리다이렉트

3. (프론트) successUrl 페이지에서 쿼리파라미터를 읽어 백엔드 호출
   POST /api/v1/payments/confirm  (Authorization: Bearer <JWT>)
   body: { paymentKey, orderId(=orderUid), amount }

4. 백엔드: 소유자/금액/상태 검증 → Toss confirm API 호출
   - 성공: order.status=PAID, pay_info 저장
   - 실패: order.status=CANCELLED, 차감된 재고 복구
```

## DB 스키마 변경

`ddl.sql`에 추가됨 (mysql 프로파일은 `ddl-auto: none`이라 **실제 DB에는 수동 실행 필요**):

- `order_info.order_uid VARCHAR(26) UNIQUE NOT NULL` — ULID, Toss `orderId`로 사용 (순차 PK 노출 방지)
- `order_info.status VARCHAR(30) DEFAULT 'PENDING_PAYMENT' NOT NULL` — `PENDING_PAYMENT` / `PAID` / `CANCELLED`
- `pay_info` 테이블 신규:
  - `order_id BIGINT UNIQUE` FK → `order_info.id`
  - `payment_key VARCHAR(200) UNIQUE`
  - `amount INT`, `status VARCHAR(20)`, `method VARCHAR(30) NULL`, `approved_at DATETIME(6) NULL`

## 신규 도메인 구조

```
domain/product/    ProductInfo, ProductInfoRepository            (조회 전용, 등록 API 없음)
domain/delivery/   DeliveryInfo, DeliveryInfoRepository           (조회 전용, 등록 API 없음)
domain/order/      OrderInfo, OrderDetailInfo, OrderService, OrderController
domain/payment/    PayInfo, PaymentService, PaymentController
common/payment/toss/       TossPaymentsApi (RestClient 선언형 인터페이스)
common/config/              TossPaymentsProperties, TossPaymentsRestClientConfig
enums/                       OrderStatus, PaymentStatus
```

- 기존 `common/oauth/*` + `OAuthRestClientConfig` 패턴(RestClient + `HttpServiceProxyFactory`, 4xx/5xx 에러 핸들러)을 Toss 연동에도 그대로 재사용.
- `ResponseCodeEnum`에 `NOT_FOUND_ORDER`, `NOT_FOUND_PRODUCT`, `NOT_FOUND_DELIVERY`, `NOT_ENOUGH_STOCK`, `ALREADY_PAID_ORDER`, `PAYMENT_AMOUNT_MISMATCH`, `PAYMENT_CONFIRM_FAILED`, `ORDER_ALREADY_CANCELLED` 추가.

## API

| Method | Path | 설명 |
|---|---|---|
| POST | `/api/v1/orders` | 주문 생성 (재고 차감) |
| GET | `/api/v1/orders/{orderId}` | 주문 조회 (본인 주문만) |
| POST | `/api/v1/payments/confirm` | Toss 결제 승인 + pay_info 저장 |

모두 `@PreAuthorize("hasRole('USER')")` — `Authorization: Bearer <accessToken>` 필요.

## 핵심 설계 결정과 이유

### 1. 재고 차감 — DB 단일 UPDATE로 원자적 처리
`ProductInfoRepository.decreaseStock()`:
```sql
UPDATE product_info SET stock_count = stock_count - :count
WHERE id = :productId AND stock_count >= :count
```
영향받은 row가 0이면 `NOT_ENOUGH_STOCK`. 엔티티를 읽고 메모리에서 체크 후 다시 저장하는 방식(lost update 위험)을 쓰지 않고, DB가 원자성을 보장하도록 함. 락을 명시적으로 잡지 않아도 되고 동시성 처리량도 가장 좋음.

재고 차감 시점은 **주문 생성 시**로 단순화. 결제 확정 시점이 아니라 주문 생성 시점에 재고를 미리 확보한다.

### 2. 주문 상태(OrderStatus) + confirm 실패 시 자동 취소
- `PENDING_PAYMENT` → `PAID`(confirm 성공) / `CANCELLED`(confirm 실패)
- Toss confirm이 명확히 거절되면(`confirmToss()`의 `getOrElse` 진입) `order.markCancelled()` + `productInfoRepository.increaseStock()`로 차감된 재고를 원복하고 예외를 다시 던짐.
- **왜 노 롤백인가**: `confirmPayment()`는 `@Transactional(noRollbackFor = [ApiErrorException::class])`. 취소 로직이 예외를 던지는 로직 안에서 실행되는데, 일반적인 `@Transactional`이면 그 예외 때문에 취소/재고복구 자체가 롤백되어 버림. `noRollbackFor`로 그 커밋을 보존.
- **재시도 차단**: 한 번 `CANCELLED`가 된 주문은 같은 `orderUid`로 다시 confirm을 호출해도 `409 ORDER_ALREADY_CANCELLED`로 즉시 막힘 (Toss를 다시 호출하지 않음). 이유: 재고를 이미 복구해줬는데 재시도로 결제가 성공하면 "결제는 됐는데 재고는 안 깎인" 상태가 되는 이중 회계 버그를 방지하기 위함.
- **트레이드오프**: 카드 하나로 결제 실패하면 그 주문 전체가 죽는다. 다른 카드로 재시도하려면 새 주문을 다시 만들어야 함. (프론트에서 "주문 재시도" UX를 만들고 싶으면 이 부분을 확장해야 함 — 아직 미구현.)
- 금액 불일치(`PAYMENT_AMOUNT_MISMATCH`)는 Toss를 아예 호출하기 전에 걸러지는 검증이라 주문을 취소하지 않음 (클라이언트가 올바른 금액으로 재시도 가능).

### 3. Toss `orderId` = `order_info.order_uid` (ULID)
`order_info.id`(순차 BIGINT)를 그대로 노출하면 Toss의 6~64자 요구사항을 못 맞출 수 있고 순차 노출도 보안상 좋지 않음. `Member.uuid`와 동일한 패턴으로 ULID 컬럼을 추가 (`build.gradle.kts`에 이미 `ulid-creator` 의존성 존재).

### 4. `successUrl`은 프론트 URL이지 백엔드 API가 아님
Toss는 결제 완료 후 **브라우저**를 `successUrl?paymentKey=...&orderId=...&amount=...`로 GET 리다이렉트시킨다. 반면 `POST /api/v1/payments/confirm`은 POST + JSON body + JWT Bearer 헤더가 필요한 엔드포인트라서 Toss의 리다이렉트가 직접 호출할 수 없음. 그래서 `successUrl`은 반드시 **프론트엔드의 결제완료 페이지**여야 하고, 그 페이지의 JS가 쿼리파라미터를 읽어 로그인 세션의 JWT로 다시 백엔드 confirm API를 호출하는 구조.

## 설정 (환경변수)

`application-local.yml`, `application-dev.yml`, `application-test.yml`에 추가:
```yaml
toss:
  payments:
    secret-key: ${TOSS_PAYMENTS_SECRET_KEY:CHANGE_ME_TOSS_SECRET_KEY}
```
로컬 실행 전 `.env`(또는 셸)에 `TOSS_PAYMENTS_SECRET_KEY`(Toss 개발자센터의 **테스트** 시크릿 키, `test_sk_...`)를 넣어둘 것. `mise.toml`에는 `.env` 자동 로드 설정이 없어서 `export $(grep -v '^#' .env | xargs)` 후 실행해야 함.

## 로컬에서 테스트 결제 발생시키는 법

1. 서버 실행: `./gradlew bootRun --args='--spring.profiles.active=local,h2'` (h2 프로파일이면 실 MySQL 없이 바로 테스트 가능, `ddl-auto: create-drop`이라 스키마 자동 생성)
2. H2 콘솔(`/h2-console`, JDBC `jdbc:h2:mem:practice`)에서 테스트용 `product_info`, `delivery_info` row를 직접 INSERT (등록 API가 없으므로 수동 seed 필요)
3. Swagger(`/swagger.html`)에서 이메일 회원가입/로그인 → JWT 발급 → Authorize에 `Bearer <token>` 등록
4. `POST /api/v1/orders` 호출 → `orderUid`, `totalPrice` 확보 (⚠️ `order_info`에 직접 INSERT하면 안 됨 — 재고 차감/금액 계산이 API를 통해야만 정확함)
5. Toss 결제위젯을 실제로 띄울 프론트가 필요함. 세션 중 임시로 `TossPayments(clientKey).requestPayment(...)`를 호출하는 단일 HTML 테스트 페이지를 스크래치패드에 만들어뒀었음(세션 종료 후 사라지는 경로라 **맥북의 `oauth-test` 프로젝트로 옮겨 재작업 예정** — 이 저장소에는 없음). 필요한 값: 테스트 클라이언트 키(`test_ck_...`, 시크릿 키와 같은 Toss 계정), `orderId`(=orderUid), `amount`(=totalPrice).
6. 결제창에서 카드번호는 형식만 맞으면(예: `4330-1234-1234-1234`) 테스트 모드라 아무 값이나 승인됨.
7. 리다이렉트로 돌아온 `paymentKey`/`orderId`/`amount`를 그대로 `POST /api/v1/payments/confirm` 바디에 넣어 호출.
8. H2 콘솔에서 `order_info.status = 'PAID'`, `pay_info`에 row 생성 확인.

## 알려진 한계 / TODO

- **미결제 주문 방치**: 사용자가 결제창까지 가지 않고 이탈하면(confirm을 아예 안 부름) 재고가 차감된 채 주문이 영구히 `PENDING_PAYMENT`로 남음. 취소 API나 만료 배치가 없음.
- **결제 재시도 불가**: 위 "노 롤백/자동 취소" 설계상 confirm이 한 번 실패하면 그 주문으로는 재시도가 불가. 새 주문 생성 필요.
- **프론트 결제 테스트 페이지**: 아직 정식 프론트 프로젝트(`oauth-test`)에 반영되지 않음, 맥북에서 이어서 작업 예정.
- 상품/배송 등록 API 자체가 없음 (조회 전용) — 관리자용 CRUD는 스코프 밖.
