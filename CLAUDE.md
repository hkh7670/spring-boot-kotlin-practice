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
  order/           주문 생성/조회/취소/반품, 배송 상태 전환, 상태 이력, 만료 주문 취소 배치
    api/           OrderController(사용자), AdminOrderController(관리자: 배송/반품완료)
    event/         OrderPaidEvent/OrderCancelledEvent + Publisher/Relay/Listener (Kafka)
    service/       OrderService, OrderCancelService(+RecordService), OrderReturnService(+RecordService),
                    OrderShippingService
  payment/         Toss 결제 승인/취소
  product/         상품 목록(카테고리·검색·페이징)/상세 조회 API (QueryDSL, ProductRepositoryImpl)
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
  `OAuth2LoginFailureHandler`가 최종 리다이렉트할 프론트 URL) 기본값은 구 테스트 프론트 포트인
  `http://localhost:3000/oauth/complete`·`/oauth/error`로 고정돼 있다. `e-commerce-frontend`(기본 포트
  5173)로 로컬에서 OAuth 로그인을 테스트하려면 `.env`에 `OAUTH_FRONTEND_SUCCESS_REDIRECT_URI`/
  `OAUTH_FRONTEND_FAILURE_REDIRECT_URI`를 `http://localhost:5173/oauth/complete`·`/oauth/error`로
  오버라이드해야 한다 — 안 하면 로그인 성공 후 브라우저가 아무것도 안 뜬 3000번 포트로 리다이렉트되어
  "화면이 안 나온다."

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

프로젝트 루트 `ddl.sql`이 스키마 단일 소스, 항상 "새 DB 최초 구축" 전제로 최신 `CREATE TABLE`만 유지.
신규 테이블은 `CREATE TABLE` 추가만. 기존 테이블 컬럼/인덱스 추가는 파일 끝에 별도 `ALTER TABLE` 추가
(단, 앞부분 `CREATE TABLE`도 최종 컬럼까지 반영). 엔티티 변경 시 `ddl.sql`도 항상 같이 갱신할 것 —
`local`/`dev`는 `ddl-auto: none`이라 자동 반영 안 됨.

로컬/데모용 상품 데이터는 프로젝트 루트 `seed-data.sql`로 별도 관리 (H2 콘솔 또는 MySQL 클라이언트에서
직접 실행). `ddl.sql`과 동일하게 "새 DB 최초 구축" 전제 — 멱등성 없어 재실행 시 중복 insert됨. 원래
`CommandLineRunner`(`LocalDataSeeder`)로 앱 기동 시 자동 시딩했었는데, 앱 코드에 데모 데이터를 심는
것보다 `ddl.sql`과 같은 방식(사람이 직접 실행하는 SQL)이 일관돼서 SQL 스크립트로 교체함.
`created_datetime`/`updated_datetime`은 `BaseTimeEntity`(JPA Auditing)가 채우는 컬럼이라 DB 기본값이
없어 `seed-data.sql`에서 직접 `NOW()`로 채워야 한다.

## 테스트

- `*ControllerTest.kt`: `MockMvc` + 실제 H2 통합 테스트. 외부 API(`TossPaymentsApi`, OAuth 클라이언트)와
  `RedisRepository`만 `@MockitoBean`. `profile=test`, H2 + `create-drop`.
- 결제완료 주문 픽스처는 `productRepository.decreaseStock()`(벌크쿼리)를 트랜잭션 밖에서 직접 호출하면
  `TransactionRequiredException` → `product.stockCount` 직접 감소+save 헬퍼(`createPaidOrder`) 사용.
- **테스트 공백** (향후 보강 필요, 요청 전엔 먼저 손대지 않기): `AuthService` reissue/rotation,
  `AuthEmailController` login/signup, `OrderShippingService`/`OrderReturnService` 전체(구현만 하고
  비용 문제로 테스트 미작성), `ProductController`/`CategoryController`/`DeliveryOptionController`(신규
  조회 API) — 전부 구현만 하고 테스트 미작성.

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
