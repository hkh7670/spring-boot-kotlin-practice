# spring-boot-kotlin-practice

Kotlin + Spring Boot 3 학습/실습용 프로젝트. OAuth 로그인, JWT 인증, 주문/결제(Toss Payments), Kafka
이벤트 발행을 직접 구현하며 Spring 생태계를 익히는 것이 목적이다.

## 기술 스택

- Kotlin 2.4.10 / Java 21 (`kotlin("plugin.spring")`, `kotlin("plugin.jpa")`, `kapt`), Gradle Kotlin DSL
- Spring Boot 3.5.16 (web, data-jpa, security, validation, data-redis)
- DB: MySQL(운영/dev), H2(local/test) — QueryDSL(OpenFeign jakarta 포크)
- 인증: JWT(`jjwt`, HMAC) + Redis(refresh token rotation) + Spring Security
- 외부 연동: Google/Kakao/Naver OAuth(PKCE), Toss Payments, Spring Kafka(외부 Docker 브로커)
- API 문서: springdoc-openapi (`/swagger.html`)
- ULID(`ulid-creator`) — 외부 노출용 식별자(`orderUid` 등)

## 빌드 / 실행

**Gradle 명령은 항상 `mise exec --`를 앞에 붙인다** (`mise.toml`이 `java = temurin-21` 고정, 안 붙이면
앰비언트 JDK와 버전이 어긋나 `kotlin("plugin.spring")`이 크래시한다).

```bash
mise exec -- ./gradlew compileKotlin compileTestKotlin
mise exec -- ./gradlew test
mise exec -- ./gradlew bootRun --args='--spring.profiles.active=local'
```

프로파일: `local`/`dev`/`mysql`은 외부 MySQL + `ddl-auto: none`(스키마 변경 시 `ddl.sql`을 사람이
직접 실행). `h2`/`test`는 H2 + `create-drop`(자동 스키마, 배포 DB 반영 확인 수단 아님). 민감정보는
gitignore된 `.env`.

## 포맷팅

IntelliJ Google Style XML import. 실제 적용 결과는 한 줄 100자(전역 120자보다 우선), Kotlin 4-space 들여쓰기.

## 패키지 구조

```
common/            도메인 무관 공통 코드 (config, security(JWT), oauth/{google,kakao,naver},
                    payment/toss(TossPaymentsApi, TossPaymentCanceller), redis, converter(AES),
                    dto(CommonResponse), entity(BaseTimeEntity))
domain/
  auth/            로그인/회원가입/토큰 재발급
  member/          회원 조회
  order/           주문 생성/목록·단건 조회/취소/반품, 배송 상태 전환, 상태 이력, 만료 주문 취소 배치
    api/           OrderController(사용자), AdminOrderController(관리자: 배송/반품완료)
    event/         OrderPaidEvent/OrderCancelledEvent + Publisher/Relay/Listener (Kafka)
    service/       OrderService, OrderCancelService(+RecordService), OrderReturnService(+RecordService),
                    OrderShippingService
  payment/         Toss 결제 승인/취소
  product/         상품 목록(카테고리·검색·페이징)/상세 조회 API (QueryDSL, ProductRepositoryImpl),
                    ProductOption(상품 옵션, 재고는 여기서만 관리)
  category/        카테고리(대/중/소분류) 목록 조회 API
  delivery/        배송 옵션 목록 조회 API
enums/             ResponseCodeEnum, OrderStatus, PaymentStatus, JoinProvider, Role, TokenType
exception/         ApiErrorException, ApiCommonAdvice
```

각 도메인은 `api/dto/entity/repository/service` 구조. Controller → Service → Repository 레이어 엄격
준수, Entity를 API 응답에 직접 노출하지 않음(DTO 변환).

## 공통 컨벤션

- 엔티티 생성은 companion object의 `of()`/`ofOAuth()` 팩토리로만 (public 생성자 직접 호출 금지).
- 에러는 `ApiErrorException(ResponseCodeEnum.XXX)`로 던진다 (`check()`/`require()` 대신).
- PII(이름/전화번호/이메일)는 `@Convert(Aes256Converter)`로 결정적 암호화(고정 IV, `WHERE` 동등조회용
  — 취약점 아니라 의도된 트레이드오프, 랜덤 IV로 바꾸지 말 것). 비밀번호는 BCrypt.
- 모든 엔티티는 `BaseTimeEntity` 상속.
- FK 컬럼/필드명은 `참조테이블명(단수) + _id`.
- Spring Data 파생 쿼리 메서드명은 **DB 컬럼명이 아니라 Kotlin 프로퍼티 경로**와 일치해야 한다 (틀리면
  `PropertyReferenceException`으로 컨텍스트 기동 자체가 실패, `@SpringBootTest` 전체 도미노 실패).
- null/empty 체크는 `CollectionUtils.isEmpty()`/`StringUtils.hasText()`, `!!` 금지.
- 상태값(`Order.status`, `Payment.status`)은 컴파일타임 강제(`private set`)가 아니라 평범한 public
  `var`다 — Kotlin 주 생성자 프로퍼티는 커스텀 접근자를 문법적으로 붙일 수 없기 때문. 대신 항상 이름
  있는 메서드(`markPaid()`, `cancelPaidOrder()`, `cancel()`, `done()` 등)로만 변경하는 컨벤션으로 대체.

## 인증/인가 (`domain/auth`, `common/security`)

- 가입 경로 2가지: EMAIL(BCrypt) / OAuth(GOOGLE·KAKAO·NAVER, PKCE 통일 — Kakao/Naver는 서버 보관
  `client_secret`으로 대체). 유니크 제약이 `(provider_id, join_provider)`+`(email, join_provider)`라
  같은 이메일을 OAuth/EMAIL 각각 가입 가능.
- 실제 OAuth API 경로는 `/api/v1/auth/oauth` (`docs/oauth-pkce-login.md`는 구 경로로 미반영 상태).
- 토큰 3종(`TokenType`): ACCESS(30분)/REFRESH(14일)/TEMP(10분, OAuth 신규가입 중간단계).
  `JwtAuthenticationFilter`는 ACCESS_TOKEN일 때만 인증 컨텍스트를 채운다.
- Refresh Token Rotation: Redis에 최신 토큰만 유지(회원당 세션 1개), 재사용 감지 시 강제 로그아웃(401).
- JWT 서명키는 `secret`을 UTF-8 바이트 그대로 사용 (AES 쪽 `AesCryptoUtil`은 base64 디코딩 — 혼동 주의).
- `app.oauth.frontend-success-redirect-uri`/`frontend-failure-redirect-uri`(`OAuth2LoginSuccessHandler`/
  `OAuth2LoginFailureHandler`가 최종 리다이렉트할 프론트 URL) 기본값은 `http://localhost:3000/oauth/complete`·
  `/oauth/error`. `e-commerce-frontend`/`backend-test-client` 둘 다 dev 서버 기본 포트가 3000이라 이
  기본값 그대로 맞는다. 다른 포트로 프론트를 띄운다면 `.env`의 `OAUTH_FRONTEND_SUCCESS_REDIRECT_URI`/
  `OAUTH_FRONTEND_FAILURE_REDIRECT_URI`를 해당 포트로 오버라이드해야 한다 — 안 하면 로그인 성공/실패 후
  브라우저가 엉뚱한 포트로 리다이렉트되어 "화면이 안 나온다."

## 주문 상태 머신 (`OrderStatus`)

```
PENDING_PAYMENT → PAID → SHIPPING → DELIVERED → RETURNING → RETURNED
       ↓            ↓
   CANCELLED    CANCELLED
```

`CANCELLED`는 `PENDING_PAYMENT`/`PAID`에서만 가능 — 배송 시작(`SHIPPING` 이상) 후에는 결제취소 불가
(`ORDER_ALREADY_SHIPPING`), 배송완료 후 환불은 반품 절차(`RETURNING`→`RETURNED`)로만. 모든 전이는
`Order` 엔티티의 이름 있는 메서드를 통하고, 각 메서드가 허용 안 되는 현재 상태를 예외로 막는다.

## 주문취소/반품 — Toss 호출 먼저, DB는 성공 후에

`OrderCancelService.cancelOrder()`(사용자, PAID→CANCELLED)와 `OrderReturnService.completeReturn()`
(관리자, RETURNING→RETURNED) 둘 다 **Toss 취소 API 성공 후에만 DB 반영**한다 — 재고복구를 먼저
해버리면 그 사이 다른 주문이 그 재고를 선점해, Toss 실패 시 되돌릴 재고가 없어지는 문제 때문에
(DB-first + 보정 방식을 검토 후 기각). `PaymentService.confirmPayment()`(결제승인)도 동일 순서.

- `common/payment/toss/TossPaymentCanceller`: Toss 취소 호출 + 에러처리(`PAYMENT_CANCEL_FAILED`)를
  모은 공용 컴포넌트. 새 취소성 플로우 추가 시 반드시 이걸 재사용.
- 각 흐름은 "검증+외부호출" 서비스와 "DB 기록 전용" 서비스로 분리 (Toss 성공 후에만 record 서비스
  호출 → 보정 로직 불필요): `OrderCancelService`→`OrderCancelRecordService`,
  `OrderReturnService.completeReturn()`→`OrderReturnRecordService`. `requestReturn()`(사용자,
  DELIVERED→RETURNING)과 `OrderShippingService`는 외부호출이 없어 각각 단일 `@Transactional`.
- Payment 조회보다 **상태 검증을 먼저** 해야 한다 — 안 그러면 `PENDING_PAYMENT`(Payment 미존재) 취소
  시도가 `NOT_FOUND_ORDER`로 잘못 응답한다 (`ORDER_NOT_PAID`가 맞음).

## 주문/결제 기타 컨벤션

- 재고 차감/복구는 조건부 UPDATE(`decreaseStock`/`increaseStock`)로 원자적 처리, 초과판매 방지.
- 가격은 주문 시점 스냅샷(`OrderItem.price`, `Order.deliveryPrice`) 사용 — 라이브 조회값 재계산 금지.
- `OrderStatusHistory`가 모든 상태 전이를 append-only로 기록 (FK 제약 의도적으로 없음).
- `StaleOrderCancelScheduler`가 10분마다 생성 10분 초과 `PENDING_PAYMENT` 주문을 취소+재고복구.
- 결제(Toss) 확정은 외부호출(`PaymentService`, `@Transactional` 없음)과 DB쓰기(`PaymentRecordService`,
  별도 빈 필수 — self-invocation은 `@Transactional` 무시됨)를 분리. `cancelOrderAndRestoreStock()`은
  결제실패 취소/만료주문 배치취소 공용.

## 상품/카테고리/배송옵션 조회 API (읽기 전용)

`e-commerce-frontend`(쿠팡 스타일 쇼핑몰) 상품 카탈로그용으로 추가된 공개 API. 전부 `SecurityConfig`의
`PERMIT_ALL_PATHS`에 등록되어 인증 불필요.

- `GET /api/v1/products` (카테고리/검색어/페이징), `GET /api/v1/products/{id}`,
  `GET /api/v1/categories`, `GET /api/v1/delivery-options`.
- `ProductRepositoryImpl.search()`(QueryDSL): count 쿼리를 먼저 실행해 0건이면 content 쿼리 자체를
  스킵 — 카테고리/검색어 결과가 없는 흔한 케이스에서 불필요한 쿼리 한 번을 아낀다.
- `ProductService.getProducts()`: 상품 목록에 표시할 `vendorName`을 상품별로 조회하지 않고
  `vendorRepository.findAllById()`로 배치 조회 후 `Map`으로 매칭 (N+1 방지, 쿼리 2번 고정).
- `OrderService.getOrders()`(주문 목록, 사용자 인증 필요): 마찬가지로 `OrderItemRepository.findByOrderIdIn()`
  배치조회로 대표 상품명 + 건수만 요약해 반환.

## 상품 옵션(`ProductOption`) — 가격/재고 둘 다 옵션 단위로만 관리

상품구매 시 옵션(사이즈/색상 등)을 지정할 수 있도록, `products` 하위에 `product_options`
테이블(1:N)을 추가하고 **가격과 재고 둘 다** 옵션 레벨로 이전했다. **모든 `Product`는 최소
1개의 `ProductOption`을 가진다** — 옵션이 실제로 없는 단순 상품도 "기본" 옵션 1개로 취급
(하이브리드 아님, 조회/차감 경로가 항상 하나로 통일됨). `products.stock_count`/`price` 컬럼은
완전히 제거됨 — `Product`에는 이제 이름/설명/이미지/카테고리/업체 정보만 남는다.

- `ProductOption`은 `OrderItem`과 동일하게(이 코드베이스에서 `@ManyToOne`을 쓰는 유이한 두 엔티티)
  `Product`를 `@ManyToOne(FetchType.LAZY)`로 참조한다 — `Product`/`Category`/`CartItem`처럼 raw
  `Long` FK를 쓰는 스타일과 의도적으로 다름(부모 엔티티 접근이 빈번해 프록시 재사용 가치가 큼).
  `Product`에는 `@OneToMany` 컬렉션을 추가하지 않았다(불필요, `ProductOptionRepository.findByProductId()`로 조회).
- 재고 차감/복구(`decreaseStock`/`increaseStock`, 조건부 원자적 UPDATE)가 `ProductRepository`에서
  `ProductOptionRepository`로 완전히 이동. `OrderItem.product` 필드도 `OrderItem.productOption`으로
  교체(FK `order_items.product_option_id`) — 상품 정보는 `orderItem.productOption.product`로 접근.
  가격도 `productOption.price`가 유일한 소스 — `OrderService.createOrder()`의 총액 계산과
  `OrderItem.price` 스냅샷 둘 다 여기서만 읽는다.
- `GET /api/v1/products/{id}` 응답은 스칼라 `price`/`stockCount` 대신 `productOptions: [{id,
  name, price, stockCount}]` 배열만 반환 (옵션 선택 UI 근거, 옵션마다 가격이 다를 수 있음).
  목록(`GET /api/v1/products`)의 `price`/`stockCount`는 필드명은 그대로지만 값은
  `ProductOptionRepository.findAggregatesByProductIdIn()`(옵션별 재고 SUM + 최저가 MIN을 한
  쿼리로 같이 집계)로 구한 "최저가/총재고" — 옵션을 아직 안 고른 목록 화면에서 실제 쇼핑몰의
  "OO원부터" 표시와 동일한 패턴.
- `ProductOptionRepository`에 fetch join 메서드 2개(`findByIdFetchProduct`/`findByIdInFetchProduct`)를
  둬서 옵션 조회 시 부모 `Product`를 한 번에 가져온다 (주문 생성 시 `productOption.product.name`
  등 상품 메타정보 접근, 장바구니 조회 양쪽에서 N+1 없이 가능 — 가격 자체는 `Product`가 아니라
  `productOption.price`에서 바로 나오므로 이 fetch join과 무관).

## 장바구니 API (`domain/cart`)

초기에는 서버 Cart 없이 프론트 zustand + localStorage로만 관리했으나(`OrderCreateRequest`가 아이템
목록을 직접 받는 구조라 가능했음), 기기 간 동기화와 재고 기반 검증이 필요해져 회원별 서버 저장 방식으로
전환함.

- `cart_items` 테이블: `(member_id, product_option_id)` UNIQUE — `CartItem` 엔티티는 `Product`/`Category`와
  동일하게 `@ManyToOne` 관계가 아닌 raw `Long` FK(`memberId`, `productOptionId`)를 쓴다(상품 옵션
  도입 전에는 `productId`였음). 목록 조회 시 `ProductOptionRepository.findByIdInFetchProduct()`로
  배치 fetch join 해 N+1을 피하는 서비스 레이어 패턴과 짝을 이루기 위함.
- API: `GET /api/v1/cart`(조회), `POST /api/v1/cart/items`(담기), `PATCH /api/v1/cart/items/{productOptionId}`
  (수량변경), `DELETE /api/v1/cart/items/{productOptionId}`(삭제) — 넷 다 "상품"이 아니라 "상품 옵션"
  단위로 동작한다. 인증 필요(`hasRole('USER')`), 담기/수량변경/삭제 액션마다 프론트가 즉시 호출해 DB에
  반영하는 구조(별도 "저장" 버튼 없음, 네이버/쿠팡과 동일한 방식).
- **담기=증분, 수량변경=절대값, 삭제=멱등**: `POST`는 이미 담겨 있으면 수량을 더하고(상품상세 "N개 더
  담기" 시맨틱), `PATCH`는 지정한 값으로 덮어쓴다. `DELETE`는 대상이 이미 없어도 에러 없이 성공 처리한다
  (멱등한 REST 삭제 시맨틱, 프론트 재시도/레이스에 안전).
- **에러 처리 정책**: 조회는 담긴 게 없어도 에러 없이 빈 배열을 반환한다. 수량변경(`PATCH`)은 대상이
  장바구니에 없으면 `NOT_FOUND_CART_ITEM`(1021)을 던진다 — 사용자가 명시적으로 "업데이트 시점엔 에러가
  필요하다"고 판단해 조회(관대)와 변경(엄격)의 정책을 다르게 가져감.
- **재고는 검증만, 예약/차감 안 함**: 담기/수량변경 시 `요청 수량 > productOption.stockCount`면
  `NOT_ENOUGH_STOCK`(1005)으로 막지만, 실제 재고를 차감하지는 않는다 — 차감은 기존 설계 그대로 주문
  생성 시점(`ProductOptionRepository.decreaseStock()` 원자적 UPDATE)에만 일어난다. 장바구니에 담아둔
  사이 재고가 줄어드는 레이스는 주문 생성 시 재검증되므로 안전(위 "주문취소/반품 — Toss 호출 먼저"
  섹션의 설계 철학과 동일).
- **`soldOut` boolean만 노출, 원본 재고 수량은 응답에 없음**: `CartItemResponse.soldOut = stockCount <= 0`
  만 내려주고 실제 `stockCount`는 필드 자체가 없다 — 프론트가 재고 수량을 임의로 추측/노출하지 못하게
  막기 위함(품절 배지 표시 용도로만 쓰라는 의도).

## Kafka (`domain/order/event`)

`order.paid`/`order.cancelled` 토픽에 발행 (`.env`의 `KAFKA_BOOTSTRAP_SERVERS`).

- `@Transactional` 메서드 내부(`PaymentRecordService`)에서는 `ApplicationEventPublisher.publishEvent()`로
  일반 이벤트만 발행 → `OrderEventRelay`가 `@TransactionalEventListener(AFTER_COMMIT)`로 받아 실제 Kafka
  발행 (롤백 시 발행 안 됨). 이미 트랜잭션 밖인 코드(`OrderCancelService`)는 `OrderEventPublisher`를 직접 호출.
- `OrderEventPublisher.send()`는 실패를 절대 상위로 전파하지 않는다(`kafkaTemplate.send()` 자체 호출과
  `whenComplete` 콜백 양쪽 다 `runCatching`) — AFTER_COMMIT 경로는 Spring이 예외를 삼켜주지만, 직접
  호출 경로는 안 삼켜서 브로커 장애가 이미 성공한 API를 500으로 만들 수 있었기 때문.
- 새 프로젝트에서 같은 브로커를 다른 `group-id`로 구독하면 독립적으로 전체 스트림 수신 가능 (같은
  group-id면 경쟁 컨슈머). 이벤트 DTO 구조가 양쪽에서 일치해야 함.
- 테스트에서는 `listener.auto-startup: false` + `max.block.ms: 2000`로 브로커 없이도 빠르게 기동.

## DB 스키마 (`ddl.sql`)

프로젝트 루트 `ddl.sql`이 스키마 단일 소스, 항상 "새 DB 최초 구축" 전제로 최신 `CREATE TABLE`만 유지
(모든 `CREATE TABLE`은 항상 최종 컬럼 상태 — 과거 컬럼 추가/삭제/리네임 흔적을 남기지 않는다).
엔티티 변경 시 `ddl.sql`도 항상 같이 갱신할 것 — `local`/`dev`는 `ddl-auto: none`이라 자동 반영
안 됨.

기존에 이미 떠 있는 real DB에 반영할 때 필요한 `ALTER TABLE`은 파일 끝에 임시로 적어두고 사람이
직접 실행한 뒤 **바로 지운다** — 다음 스키마 변경 때 그대로 남겨두면, 나중에 `CREATE TABLE`에
이미 흡수된 컬럼을 다시 추가하려 들거나(중복 컬럼 에러) 이미 이름이 바뀐/삭제된 컬럼을 참조하게
되어(unknown column 에러), 신규 DB에 CREATE부터 전체를 순서대로 실행할 때 중간에 깨진다(실제로
겪은 사고: `products.stock_count`/`price` 제거와 `cart_items`/`order_items`의
`product_id`→`product_option_id` 리네임이 `CREATE TABLE`엔 반영됐는데 파일 끝 `ALTER TABLE`은
옛 상태 그대로 남아있어 총돌). 즉 `ALTER TABLE` 블록은 "지금 막 반영해야 하는 사람을 위한 1회용
안내문"이지 히스토리 기록이 아니다.

로컬/데모용 상품 데이터는 프로젝트 루트 `seed-data.sql`로 별도 관리 (H2 콘솔 또는 MySQL 클라이언트에서
직접 실행). `ddl.sql`과 동일하게 "새 DB 최초 구축" 전제 — 멱등성 없어 재실행 시 중복 insert됨. 원래
`CommandLineRunner`(`LocalDataSeeder`)로 앱 기동 시 자동 시딩했었는데, 앱 코드에 데모 데이터를 심는
것보다 `ddl.sql`과 같은 방식(사람이 직접 실행하는 SQL)이 일관돼서 SQL 스크립트로 교체함.
`created_datetime`/`updated_datetime`은 `BaseTimeEntity`(JPA Auditing)가 채우는 컬럼이라 DB 기본값이
없어 `seed-data.sql`에서 직접 `NOW()`로 채워야 한다.

## 테스트

- `*ControllerTest.kt`: `MockMvc` + 실제 H2 통합 테스트. 외부 API(`TossPaymentsApi`, OAuth 클라이언트)와
  `RedisRepository`만 `@MockitoBean`. `profile=test`, H2 + `create-drop`.
- 결제완료 주문 픽스처는 `productOptionRepository.decreaseStock()`(벌크쿼리)를 트랜잭션 밖에서 직접
  호출하면 `TransactionRequiredException` → `productOption.stockCount` 직접 감소+save 헬퍼
  (`createPaidOrder`) 사용.
- **테스트 공백** (향후 보강 필요, 요청 전엔 먼저 손대지 않기): `AuthService` reissue/rotation,
  `AuthEmailController` login/signup, `OrderShippingService`/`OrderReturnService` 전체(구현만 하고
  비용 문제로 테스트 미작성), `ProductController`/`CategoryController`/`DeliveryOptionController`(신규
  조회 API), `OrderController.getOrders()`(목록 API), `CartController`(장바구니 조회/담기/수량변경/삭제)
  — 전부 구현만 하고 테스트 미작성.

## 배포

- 로컬: `bootRun --spring.profiles.active=local` (port 16000).
- 운영: 맥미니 자가호스팅(`http://hkh7670.iptime.org:8080`), `dev` 프로파일, nginx 리버스 프록시 뒤
  무중단 배포(`forward-headers-strategy: framework`).
- CORS는 `SecurityConfig.corsConfigurationSource()` 화이트리스트 방식 — 새 프론트엔드/배포 도메인 추가
  시 여기 등록 필요 (동일 origin 호출은 CORS 검사 자체가 발동하지 않아 "로컬은 되는데 배포는 안 됨"
  증상의 전형적 원인).

## 관련 프로젝트 / 문서

- `/Users/kyu/workspace/e-commerce-frontend`: 쿠팡 스타일 쇼핑몰 메인 프론트엔드(React+Vite+TS). 홈/상품
  목록·검색/상품상세/장바구니/주문·결제(Toss)/주문내역·취소·반품/로그인(이메일+OAuth) 전체 구현. 백엔드
  API 변경 시 같이 갱신 필요. 자체 CLAUDE.md 참고.
- `/Users/kyu/workspace/backend-test-client`: 수동 테스트용 Vite+React+TS 프론트엔드(OAuth 로그인,
  주문, Toss 결제 테스트). 백엔드 API 변경 시 필요하면 같이 갱신.
- `docs/oauth-pkce-login.md`: OAuth PKCE 흐름 (API 베이스 경로는 구버전, 위 인증 섹션 참고).
- `docs/order-toss-payment-integration.md`: 주문/Toss 결제 연동 설계 문서.
