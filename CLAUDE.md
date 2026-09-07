# spring-boot-kotlin-practice

Kotlin + Spring Boot 4 학습/실습 프로젝트. OAuth 로그인, JWT 인증, 주문/결제(Toss Payments), Kafka
이벤트 발행을 직접 구현하며 Spring 생태계를 익히는 것이 목적.

## 기술 스택

- Kotlin 2.4.10 / JDK 25 (`kotlin("plugin.spring")`, `kotlin("plugin.jpa")`, `kapt`), Gradle 9.7.1 Kotlin DSL
- Spring Boot 4.1.1 (Spring Framework 7, Jakarta EE 11) — web, data-jpa, security, validation, data-redis
- DB: MySQL(운영/dev), H2(local/test) — QueryDSL(OpenFeign jakarta 포크, 7.6+)
- 인증: JWT(`jjwt`, HMAC) + Redis(refresh token rotation) + Spring Security
- 외부 연동: Google/Kakao/Naver OAuth(PKCE), Toss Payments, Spring Kafka(외부 Docker 브로커)
- API 문서: springdoc-openapi 3.x (`/swagger.html`)
- ULID(`ulid-creator`) — 외부 노출용 식별자(`orderUid` 등)

### Spring Boot 4 마이그레이션 메모 (3.5.16→4.1.1, JDK 21→25, 2026-09)

Boot 4는 기존 단일 `spring-boot-autoconfigure` jar를 기능별 수십 개 모듈로 쪼갬 — starter 없이 순수
라이브러리만 의존성으로 추가하면 그 기능의 autoconfigure 모듈이 안 딸려와 런타임에만 조용히 실패한다
(컴파일은 되는 경우도 있어 더 위험). 겪은 것들:

- `PathRequest`: `o.s.boot.autoconfigure.security.servlet` → `o.s.boot.security.autoconfigure.web.servlet`로
  패키지 이동
- `@AutoConfigureMockMvc`: `spring-boot-test-autoconfigure`에서 완전히 빠짐 — 신규 모듈
  `spring-boot-webmvc-test`(패키지도 `o.s.boot.webmvc.test.autoconfigure`로 이동)를
  `testImplementation`으로 별도 추가해야 함
- H2 콘솔(`PathRequest.toH2Console()`)도 별도 모듈 `spring-boot-h2console`로 분리 — `runtimeOnly` 추가
- `org.springframework.kafka:spring-kafka`를 스타터 없이 raw로만 넣으면 `KafkaTemplate` 빈 자체가 안
  생김 — 신규 공식 스타터 `org.springframework.boot:spring-boot-starter-kafka`로 교체
- Jackson 기본값이 Jackson 3(`tools.jackson.*`)로 바뀌어 `com.fasterxml.jackson.databind.ObjectMapper`
  빈이 기본으로 안 생김. Jackson 3 전면 마이그레이션은 범위 밖이라 공식 가이드의 "임시 Jackson 2 유지"
  경로 채택 — `org.springframework.boot:spring-boot-jackson2` 추가로 classic `ObjectMapper` 복원.
  단, Kotlin `val isXxx: Boolean` 프로퍼티의 JSON 필드명이 실제 응답을 태우는 스택에 따라 달라질 수
  있어(jackson-module-kotlin의 "is" 유지 특수처리가 Jackson 3 경로엔 없음) API 계약상 중요한 필드는
  `@get:JsonProperty("isXxx")`로 명시 고정할 것(`OrderDetailResponse.isPaid` 참고)
- Spring Security 7: 람다 DSL(`authorizeHttpRequests`, `.oauth2Login {}` 등)은 그대로지만
  `OidcUserInfo.subject`/`userNameAttributeName`/`PasswordEncoder.encode()` 반환값 등 일부 API의
  nullability가 엄격해짐(`-Xjsr305=strict`라 컴파일타임에 드러남) — 전부 외부 시스템발 이상으로 간주해
  `ApiErrorException(EXTERNAL_SERVER_ERROR / INTERNAL_SERVER_ERROR)`로 처리
- Gradle 8.14.5는 JDK 25 위에서 데몬 자체가 기동 안 됨(에러 메시지도 불명확) — Gradle 9.7.1로 wrapper
  업그레이드 필요
- Spring Framework 7의 테스트 컨텍스트 "restart" 경로(캐시된 컨텍스트를 여러 테스트 클래스가 재사용할
  때)는 `spring.kafka.listener.auto-startup: false`를 우회해 리스너 시작을 시도함 — 테스트 프로파일에
  `spring.kafka.consumer.group-id`가 원래 빠져있던 잠재적 설정 누락이 이때 처음 드러남(지금까지는
  리스너가 아예 안 뜨니 안 걸렸을 뿐). 테스트 kafka 설정에도 운영과 동일하게 group-id를 채워둘 것
- `springdoc.show-actuator: true`(`application-local.yml`/`application-dev.yml`)가 부팅 자체를
  깨뜨림 — actuator 의존성이 없는 프로젝트라 원래도 쓸모없던 설정이었는데, springdoc 3.1.0의
  `actuatorProvider` 빈이 `ManagementServerProperties`(actuator 전용 클래스)를 제네릭 파라미터로
  참조하는 과정에서 `ClassNotFoundException`으로 전체 컨텍스트 기동이 실패함. 테스트(MockMvc)는 이
  프로파일을 안 타서 안 걸리고 실제 `bootRun`에서만 드러남 — actuator 도입 전까지는 `false` 유지

## 빌드 / 실행

**Gradle 명령엔 항상 `mise exec --`를 붙인다** — `mise.toml`이 `java = temurin-21` 고정, 안 붙이면
앰비언트 JDK와 버전이 어긋나 `kotlin("plugin.spring")`이 크래시.

```bash
mise exec -- ./gradlew compileKotlin compileTestKotlin
mise exec -- ./gradlew test
mise exec -- ./gradlew bootRun --args='--spring.profiles.active=local'
```

- `local`/`dev`/`mysql`: 외부 MySQL + `ddl-auto: none`(스키마 변경 시 `ddl.sql`을 사람이 직접 실행)
- `h2`/`test`: H2 + `create-drop`(자동 스키마, 배포 DB 반영 확인 수단 아님)
- 민감정보는 gitignore된 `.env`

## 포맷팅

IntelliJ Google Style XML import, Kotlin 4-space 들여쓰기. 한 줄 100자 제한은 `.editorconfig`에
명시(IntelliJ가 자동 인식, CLI 강제는 아직 없음 — 필요해지면 ktlint 도입 검토).

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

각 도메인 `api/dto/entity/repository/service` 구조. Controller → Service → Repository 엄격 준수,
Entity를 API 응답에 직접 노출 안 함(DTO 변환).

## 공통 컨벤션

- 엔티티 생성: companion object `of()`/`ofOAuth()` 팩토리만 (public 생성자 직접 호출 금지)
- 에러: `ApiErrorException(ResponseCodeEnum.XXX)`로 던짐 (`check()`/`require()` 금지)
- PII(이름/전화번호/이메일): `@Convert(Aes256Converter)`로 결정적 암호화(고정 IV, `WHERE` 동등조회용
  — 취약점 아니라 의도된 트레이드오프, 랜덤 IV로 바꾸지 말 것). 비밀번호는 BCrypt
- 모든 엔티티: `BaseTimeEntity` 상속
- FK 컬럼/필드명: `참조테이블명(단수) + _id`
- Spring Data 파생 쿼리 메서드명: **DB 컬럼명이 아니라 Kotlin 프로퍼티 경로**와 일치 필수 (틀리면
  `PropertyReferenceException`으로 컨텍스트 기동 자체가 실패 → `@SpringBootTest` 전체 도미노 실패)
- null/empty 체크: `CollectionUtils.isEmpty()`/`StringUtils.hasText()` 사용, `!!` 금지
- 상태값(`Order.status`, `Payment.status`): 평범한 public `var`(Kotlin 주 생성자 프로퍼티는 커스텀
  접근자를 문법적으로 못 붙임) — 대신 항상 이름 있는 메서드(`markPaid()`, `markShipping()`, `cancel()`
  등)로만 변경. 단, 외부 API(Toss) 성공 후 반영되는 동시성 민감 전이(결제확정/주문취소/반품완료)는
  `OrderRepository.updateStatusIfCurrent()` 원자적 조건부 UPDATE 사용 — 아래 "주문취소/반품" 참고

## 인증/인가 (`domain/auth`, `common/security`)

- 가입 경로 2가지: EMAIL(BCrypt) / OAuth(GOOGLE·KAKAO·NAVER, PKCE 통일 — Kakao/Naver는 서버 보관
  `client_secret`으로 대체). 유니크 제약 `(provider_id, join_provider)` + `(email, join_provider)` →
  같은 이메일로 OAuth/EMAIL 각각 가입 가능
- OAuth API 경로: `/api/v1/auth/oauth`
- OAuth 로그인 흐름 (`oauth2Login` + 1회용 relay code):
  - 프론트는 전체 페이지 이동으로 `/oauth2/authorization/{provider}` 진입(fetch 아님, CORS 영향 없음)
  - `redirect_uri`는 항상 백엔드 도메인 — Provider는 프론트 도메인을 모름
  - 콜백 처리 후 `OAuth2LoginSuccessHandler`가 로그인 결과(JWT 또는 tempToken)를
    `OAuthRelayCodeService`로 Redis에 UUID 키·60초 TTL로 저장, 그 UUID만 리다이렉트 URL에 실음 —
    **JWT가 URL에 노출되는 일 없음**
  - 프론트(`/oauth/complete?code=...`)가 `POST /api/v1/auth/oauth/exchange`로 1회 소비(조회 즉시
    Redis에서 삭제)
  - 로그인 왕복 구간에서만 세션 쿠키 사용, 이후 API 인증은 완전 stateless JWT
- 토큰 4종(`TokenType`): ACCESS(30분) / REFRESH(14일) / TEMP(10분, OAuth 신규가입 중간단계) /
  TOTP_PENDING(10분, EMAIL 로그인 2단계 인증 중간단계). `JwtAuthenticationFilter`는 ACCESS_TOKEN만
  인증 컨텍스트를 채움
- Refresh Token Rotation: Redis에 최신 토큰만 유지(회원당 세션 1개), 재사용 감지 시 강제 로그아웃(401)
- JWT 서명키는 `secret`을 UTF-8 바이트 그대로 사용 (AES `AesCryptoUtil`은 base64 디코딩 — 혼동 주의)
- `app.oauth.frontend-success/failure-redirect-uri` 기본값 `localhost:3000/oauth/complete`·
  `/oauth/error` — 프론트가 3000 아닌 포트면 `.env`의 `OAUTH_FRONTEND_*_REDIRECT_URI`를 오버라이드
  필요(안 하면 로그인 성공/실패 후 엉뚱한 포트로 리다이렉트되어 "화면이 안 나온다")

### TOTP(OTP 앱) 2단계 인증 — EMAIL 로그인 전용, 선택 기능

이메일 인증코드 대신 TOTP(RFC 6238, `dev.samstevens.totp`) 채택 — 메일 발송 인프라가 전무해 이메일
코드 방식은 처음부터 구축해야 하는 반면 TOTP는 라이브러리 하나로 끝남. 강제 아님,
`Member.totpEnabled`로 회원이 계정 설정에서 on/off.

- `AuthService.login()`: OAuth의 LOGIN/NEED_SIGN_UP과 동일한 상태분기 — `EmailLoginResponse.status`가
  `LOGIN`(TOTP 미사용, 즉시 access/refresh 발급) / `NEED_TOTP`(TOTP 사용, `totpPendingToken`만 발급,
  최종 토큰은 `POST /api/v1/auth/totp/login`). **Breaking change**: `/api/v1/auth/email/login` 응답이
  `AuthTokenResponse`(`{grantType, accessToken, refreshToken}`)에서 `EmailLoginResponse`
  (`{status, totpPendingToken?, accessToken?, refreshToken?}`)로 변경 — TOTP 미사용 회원도 새 포맷 적용
- 등록/해제: `TotpController`(`/enroll`, `/enroll/confirm`, `/disable`, 모두 인증 필요) → `TotpService`.
  확정 전 시크릿은 `Member`에 바로 안 쓰고 Redis에만(`totp-enroll:{memberId}`, 5분 TTL), 첫 코드 검증
  성공 시에만 `Member.enableTotp()`로 영구 저장 — 잘못된 등록 도중 이탈로 계정이 잠기는 것 방지
- `Member.totpSecret`은 PII 필드와 동일한 `Aes256Converter`(고정 IV)로 암호화 저장 — 동등조회가 필요
  없는 시크릿엔 원래 랜덤 IV가 더 적합하나, 기존 컨버터 재사용을 우선한 의도적 트레이드오프
- 로그인 중간 토큰은 OAuth `TEMP_TOKEN`과 클레임 모양이 달라(`TempTokenClaims`는
  `providerId`/`provider`/`email`/`nickname`, TOTP pending은 `{id, tokenType}`뿐) 혼용 위험을 피하려
  별도 `TokenType.TOTP_PENDING_TOKEN`으로 분리. 클레임은 REFRESH_TOKEN과 동일해 `JwtTokenClaims.of(id,
  tokenType)` 재사용, 유효기간도 기존 `jwtProperties.tempTokenValidityMs` 재사용

## 주문 상태 머신 (`OrderStatus`)

```
PENDING_PAYMENT → PAID → SHIPPING → DELIVERED → RETURNING → RETURNED
       ↓            ↓
   CANCELLED    CANCELLED
```

- `CANCELLED`: `PENDING_PAYMENT`/`PAID`에서만 가능 — 배송 시작(`SHIPPING` 이상) 후엔 결제취소 불가
  (`ORDER_ALREADY_SHIPPING`), 배송완료 후 환불은 반품 절차(`RETURNING`→`RETURNED`)로만
- 단순 전이(배송 시작/완료, 반품요청): `Order` 엔티티 이름 있는 메서드가 허용 안 되는 상태를 예외로 차단
- 외부 API(Toss) 성공 후 반영되는 전이(결제확정/취소/반품완료): `OrderRepository.updateStatusIfCurrent()`
  원자적 조건부 UPDATE가 차단 — 아래 "동시 중복 요청 방어" 참고

## 주문취소/반품 — Toss 호출 먼저, DB는 성공 후에

`OrderCancelService.cancelOrder()`(PAID→CANCELLED), `OrderReturnService.completeReturn()`
(RETURNING→RETURNED), `PaymentService.confirmPayment()`(결제승인) 모두 **Toss API 성공 후에만 DB
반영** — 재고복구를 먼저 하면 그 사이 다른 주문이 선점해, Toss 실패 시 되돌릴 재고가 없어짐
(DB-first + 보정 방식은 검토 후 기각).

- `common/payment/toss/TossPaymentCanceller`: Toss 취소 호출 + 에러처리(`PAYMENT_CANCEL_FAILED`) 공용
  컴포넌트 — 새 취소성 플로우는 반드시 이걸 재사용
- 흐름 분리: "검증+외부호출" 서비스 / "DB 기록 전용" 서비스 — `OrderCancelService`→
  `OrderCancelRecordService`, `OrderReturnService.completeReturn()`→`OrderReturnRecordService`
  (Toss 성공 후에만 record 서비스 호출 → 보정 로직 불필요). `requestReturn()`(DELIVERED→RETURNING)과
  `OrderShippingService`는 외부호출 없어 각각 단일 `@Transactional`
- Payment 조회보다 **상태 검증을 먼저** — 안 그러면 `PENDING_PAYMENT`(Payment 미존재) 취소 시도가
  `NOT_FOUND_ORDER`로 오응답(`ORDER_NOT_PAID`가 맞음)
- **동시 중복 요청 방어**: `PaymentRecordService`/`OrderCancelRecordService`/`OrderReturnRecordService`
  모두 상태 전이를 엔티티 메서드가 아니라 `OrderRepository.updateStatusIfCurrent(orderId,
  expectedStatus, newStatus)`(원자적 조건부 UPDATE, `decreaseStock()`과 동일 패턴)로 처리 — 영향
  row가 0이면 이미 다른 동시 요청이 처리한 것이므로 재고복구/이벤트발행 등 후속 부수효과를 스킵
  - 배경: 프론트(`OrderCompletePage.tsx`)가 React StrictMode 이중 마운트로 결제확정을 밀리초 단위로
    두 번 호출 → 엔티티 메서드의 in-memory 상태체크(두 트랜잭션이 같은 상태를 동시에 읽음)가 재고를
    두 배로 복구하는 사고 발생(프론트는 `useRef` 가드로 수정, 백엔드는 방어적으로 원자적 UPDATE 채택)
  - 비관적 락(`SELECT ... FOR UPDATE`)은 Toss 외부호출과 얽혀 락 경합/데드락 위험 있어 미채택
  - `Order.markCancelled()`/`cancelPaidOrder()`/`completeReturn()` 엔티티 메서드는 이 전환으로 전부
    삭제됨(각 Service의 사전 상태검증이 이미 걸러줘 손실 없음). `markPaid()`/`markShipping()`/
    `markDelivered()`/`requestReturn()`은 동시 중복 위험 없는 단순 전이라 그대로 유지

## 주문/결제 기타 컨벤션

- 재고 차감/복구: 조건부 UPDATE(`decreaseStock`/`increaseStock`)로 원자적 처리, 초과판매 방지
- `@Modifying` 벌크 UPDATE(`decreaseStock`/`increaseStock`/`updateStatusIfCurrent`)는 JPA
  Auditing(`@LastModifiedDate`)이 안 타므로(엔티티 생명주기 콜백 미경유) `updated_datetime`을 쿼리
  안에서 `NOW(6)`으로 직접 갱신해야 함. `NOW(6)`은 MySQL 네이티브 함수라 JPQL 불가 → `nativeQuery =
  true` 필수, enum은 ordinal/string 바인딩 모호성을 피하려 `.name`(문자열)으로 전달
- 가격은 주문 시점 스냅샷(`OrderItem.price`, `Order.deliveryPrice`) 사용 — 라이브 조회값 재계산 금지
- `OrderStatusHistory`가 모든 상태 전이를 append-only로 기록 (FK 제약 의도적으로 없음)
- `StaleOrderCancelScheduler`가 10분마다 생성 10분 초과 `PENDING_PAYMENT` 주문을 취소+재고복구
- 결제(Toss) 확정은 외부호출(`PaymentService`, `@Transactional` 없음)과 DB쓰기(`PaymentRecordService`,
  별도 빈 필수 — self-invocation은 `@Transactional` 무시됨)를 분리. `cancelOrderAndRestoreStock()`은
  결제실패 취소/만료주문 배치취소 공용

## 상품/카테고리/배송옵션 조회 API (읽기 전용)

`e-commerce-frontend`(쿠팡 스타일 쇼핑몰) 상품 카탈로그용 공개 API. 전부 `SecurityConfig`의
`PERMIT_ALL_PATHS`에 등록, 인증 불필요.

- `GET /api/v1/products`(카테고리/검색어/페이징), `GET /api/v1/products/{id}`,
  `GET /api/v1/categories`, `GET /api/v1/delivery-options`
- `ProductRepositoryImpl.search()`(QueryDSL): count 쿼리를 먼저 실행해 0건이면 content 쿼리 자체를
  스킵 — 카테고리/검색어 결과가 없는 흔한 케이스에서 불필요한 쿼리 한 번을 아낌
- `ProductService.getProducts()`: `vendorName`을 상품별로 조회하지 않고
  `vendorRepository.findAllById()`로 배치 조회 후 `Map`으로 매칭 (N+1 방지, 쿼리 2번 고정)
- `OrderService.getOrders()`(주문 목록): 마찬가지로 `OrderItemRepository.findByOrderIdIn()` 배치조회로
  대표 상품명 + 건수만 요약해 반환

**알려진 이슈**: `categories`는 대/중/소분류 계층 구조인데, `products.category_id`와 `ddl.sql` 컬럼
주석("소분류")은 마치 리프(소분류) 전용인 것처럼 문서화돼 있음 — 실제 DB 레벨 강제는 없어 대/중/소
어느 레벨이든 저장 가능. leaf 강제가 필요한지, 문서 표현만 고칠지 미정.

## 상품 옵션(`ProductOption`) — 가격/재고 둘 다 옵션 단위로만 관리

상품구매 시 옵션(사이즈/색상 등)을 지정할 수 있도록 `products` 하위에 `product_options`
테이블(1:N)을 두고 **가격과 재고 둘 다** 옵션 레벨로 이전. **모든 `Product`는 최소 1개의
`ProductOption`을 가짐** — 옵션이 실제로 없는 단순 상품도 "기본" 옵션 1개로 취급(조회/차감 경로가
항상 하나로 통일). `products.stock_count`/`price` 컬럼은 완전 제거 — `Product`엔 이름/설명/이미지/
카테고리/업체 정보만 남음.

- `ProductOption`은 (`OrderItem`과 함께 이 코드베이스에서 유이하게) `Product`를
  `@ManyToOne(FetchType.LAZY)`로 참조 — `Product`/`Category`/`CartItem`의 raw `Long` FK 스타일과
  의도적으로 다름(부모 엔티티 접근이 빈번해 프록시 재사용 가치가 큼). `Product`에는 `@OneToMany`
  컬렉션 미추가(불필요, `ProductOptionRepository.findByProductId()`로 조회)
- 재고 차감/복구(`decreaseStock`/`increaseStock`)가 `ProductRepository`에서 `ProductOptionRepository`로
  완전히 이동. `OrderItem.product`도 `OrderItem.productOption`으로 교체(FK
  `order_items.product_option_id`), 가격도 `productOption.price`가 유일한 소스
- `GET /api/v1/products/{id}` 응답은 스칼라 `price`/`stockCount` 대신 `productOptions: [{id, name,
  price, stockCount}]` 배열만 반환. 목록(`GET /api/v1/products`)의 `price`/`stockCount`는 필드명은
  그대로지만 값은 `findAggregatesByProductIdIn()`(옵션별 재고 SUM + 최저가 MIN을 한 쿼리로 집계)로
  구한 "최저가/총재고"
- `ProductOptionRepository`의 `findByIdFetchProduct`/`findByIdInFetchProduct`: fetch join으로 부모
  `Product`를 한 번에 가져옴(주문 생성·장바구니 조회에서 N+1 없이 상품 메타정보 접근 — 가격 자체는
  `productOption.price`에서 바로 나오므로 이 fetch join과 무관)

## 장바구니 API (`domain/cart`)

초기엔 서버 Cart 없이 프론트 zustand + localStorage로만 관리했으나(`OrderCreateRequest`가 아이템
목록을 직접 받는 구조라 가능) 기기 간 동기화·재고 기반 검증이 필요해져 회원별 서버 저장으로 전환.

- `cart_items`: `(member_id, product_option_id)` UNIQUE — `CartItem`은 `Product`/`Category`와
  동일하게 raw `Long` FK(`memberId`, `productOptionId`) 사용
- API: `GET /api/v1/cart`(조회), `POST /api/v1/cart/items`(담기),
  `PATCH /api/v1/cart/items/{productOptionId}`(수량변경), `DELETE .../{productOptionId}`(삭제) —
  넷 다 "상품 옵션" 단위, 인증 필요, 액션마다 즉시 DB 반영(별도 저장 버튼 없음)
- **담기=증분, 수량변경=절대값, 삭제=멱등**(대상이 없어도 에러 없이 성공)
- **에러 처리**: 조회는 관대(담긴 게 없어도 빈 배열). 수량변경(`PATCH`)은 대상이 없으면
  `NOT_FOUND_CART_ITEM`(1021)
- **재고는 검증만, 예약/차감 안 함**: 담기/수량변경 시 초과 요청이면 `NOT_ENOUGH_STOCK`(1005)이지만
  실제 차감은 주문 생성 시점에만(`decreaseStock()` 원자적 UPDATE) — 장바구니에 담긴 사이 재고가 줄어드는
  레이스는 주문 생성 시 재검증되므로 안전
- `CartItemResponse`는 `soldOut` boolean만 노출, 실제 `stockCount`는 응답에 없음(품절 배지 용도로만,
  재고 수량 추측/노출 방지)

## 쿠폰/포인트 (`domain/coupon`, `domain/point`)

3계층 구조로 설계: **템플릿**(`coupons`/`points`, `vendors`처럼 raw `Long` FK로 참조되는 재사용 가능한
정의) → **발급 인스턴스**(`member_coupons`/`member_points`, 상태가 바뀌는 mutable row) → **사용
내역**(`member_point_usages`, append-only). 쿠폰은 발급 인스턴스 자체가 사용 여부를 담아 별도 사용
내역 테이블이 필요 없지만, 포인트는 한 주문에서 여러 적립건에 걸쳐 나눠 차감될 수 있어 "어느 적립건에서
얼마 썼는지"를 별도로 남겨야 함.

- **쿠폰**: `MemberCoupon.status`(UNUSED/USED/EXPIRED) 하나로 사용 여부를 관리, `orderId`/`usedAt`은
  사용 시점에만 채워짐. 정액(FIXED)/정률(PERCENTAGE, `maxDiscountPrice` 상한) 두 방식,
  `minOrderPrice` 미만 주문엔 적용 불가(`Coupon.calculateDiscountPrice()`)
- **포인트 FIFO 부분차감**: 회원의 `ACTIVE` 상태 `member_points`를 만료 임박 순으로 정렬해 순차
  차감(`remaining_amount`가 0이 되면 `EXHAUSTED`로 전이), 차감할 때마다 `member_point_usages`에
  "이 적립건에서 얼마 썼는지" 기록 — 원복 시 이 테이블만 보면 정확히 되돌릴 수 있음
- **사용 시점 = 주문 생성 시점**: 재고 차감과 동일한 위치·동일한 트랜잭션에서 확정
  (`OrderService.createOrder()`). 검증(소유자/상태/만료/최소주문금액)은 서비스 레이어에서 먼저
  거르고, 실제 확정은 `decreaseStock()`/`updateStatusIfCurrent()`와 동일한 **원자적 조건부
  UPDATE**(`MemberCouponRepository.useIfUnused()`, `MemberPointRepository.deductIfEnough()`)로
  처리 — 영향 row가 0이면 검증 이후 다른 요청이 먼저 써버렸다는 뜻이라 예외를 던짐. 포인트가 필요액을
  다 못 채우면 이미 확정된 앞선 적립건 차감분도 같은 트랜잭션이라 자동 롤백됨(별도 보정 로직 불필요)
- **주문 저장 순서 문제**: 쿠폰/포인트 확정에는 `orderId`가 필요한데 주문을 저장하기 전엔 ID가 없음 →
  `Order`를 할인 0으로 먼저 저장한 뒤, 쿠폰/포인트를 확정하고 `Order.applyDiscount()`로 할인액을
  반영(별도 `save()` 없이 dirty checking으로 커밋 시 자동 UPDATE). 그래서 `couponDiscountPrice`/
  `pointDiscountPrice`는 다른 가격 스냅샷 필드와 달리 `var`(updatable 허용)
- **원복**: 주문취소(`OrderCancelRecordService`)/반품완료(`OrderReturnRecordService`)/결제실패취소
  (`PaymentRecordService.cancelOrderAndRestoreStock()`) 3곳 모두 재고 `increaseStock()` 옆에
  `CouponService.restore(orderId)`/`PointService.restore(orderId)`를 나란히 호출 — 둘 다 이 주문에
  실제 사용 이력이 없으면 조용히 no-op이라 항상 무조건 호출해도 안전
  (`restoreIfUsedByOrder()`가 0건 처리, `member_point_usages`가 비어있으면 forEach가 안 돎)
- 가격 스냅샷 컨벤션 확장: `Order.couponDiscountPrice`/`pointDiscountPrice`를
  `PaymentService.calculateExpectedAmount()`가 그대로 사용해 "요청 금액 == Toss 승인 금액 == 서버
  확정 금액" 3자 검증에 자동 반영됨(재계산 없음)
- 조회 API(둘 다 인증 필요, 결제 화면에서 사용 가능한 쿠폰/포인트를 보여주기 위함):
  `GET /api/v1/coupons`(미사용·미만료 보유 쿠폰 목록), `GET /api/v1/points`(사용 가능 잔액 합계)
- 관리자용 발급 API는 아직 없음(향후 과제) — 현재는 `seed-data.sql`로만 시딩

## Kafka (`domain/order/event`)

`order.paid`/`order.cancelled` 토픽에 발행 (`.env`의 `KAFKA_BOOTSTRAP_SERVERS`).

- `@Transactional` 메서드 내부(`PaymentRecordService`)에서는 `ApplicationEventPublisher.publishEvent()`로
  일반 이벤트만 발행 → `OrderEventRelay`가 `@TransactionalEventListener(AFTER_COMMIT)`로 받아 실제
  Kafka 발행(롤백 시 미발행). 트랜잭션 밖 코드(`OrderCancelService`)는 `OrderEventPublisher` 직접 호출
- `OrderEventPublisher.send()`는 실패를 절대 상위로 전파 안 함(`kafkaTemplate.send()`/`whenComplete`
  둘 다 `runCatching`) — AFTER_COMMIT 경로는 Spring이 예외를 삼켜주지만 직접 호출 경로는 안 삼켜서
  브로커 장애가 이미 성공한 API를 500으로 만들 수 있었기 때문
- 다른 프로젝트가 같은 브로커를 다른 `group-id`로 구독하면 독립적으로 전체 스트림 수신 가능(같은
  group-id면 경쟁 컨슈머), 이벤트 DTO 구조는 양쪽 일치 필요
- 테스트에서는 `listener.auto-startup: false` + `max.block.ms: 2000`로 브로커 없이도 빠르게 기동

## DB 스키마 (`ddl.sql`)

`ddl.sql`이 스키마 단일 소스, 항상 "새 DB 최초 구축" 전제로 최신 `CREATE TABLE`만 유지(과거 컬럼
추가/삭제/리네임 흔적을 남기지 않음). 엔티티 변경 시 항상 같이 갱신 — `local`/`dev`는 `ddl-auto: none`
이라 자동 반영 안 됨.

- 기존에 이미 떠 있는 real DB에 반영할 `ALTER TABLE`은 파일 끝에 임시로 적어두고 사람이 직접 실행한 뒤
  **바로 지운다** — 남겨두면 다음 스키마 변경 때 이미 `CREATE TABLE`에 흡수된 컬럼을 중복 추가하려
  들거나 이미 이름 바뀐/삭제된 컬럼을 참조해, 신규 DB에 CREATE부터 순서대로 실행할 때 중간에 깨짐.
  `ALTER TABLE` 블록은 "지금 막 반영해야 하는 사람을 위한 1회용 안내문"이지 히스토리 기록이 아님
- 로컬/데모용 상품 데이터는 `seed-data.sql`로 별도 관리(H2 콘솔/MySQL 클라이언트에서 직접 실행,
  멱등성 없어 재실행 시 중복 insert). `created_datetime`/`updated_datetime`은 JPA Auditing 컬럼이라
  DB 기본값이 없어 `seed-data.sql`에서 직접 `NOW()`로 채워야 함

## 테스트

- `*ControllerTest.kt`: `MockMvc` + 실제 H2 통합 테스트. 외부 API(`TossPaymentsApi`, OAuth 클라이언트)와
  `RedisRepository`만 `@MockitoBean`. `profile=test`, H2 + `create-drop`
- 결제완료 주문 픽스처는 `productOptionRepository.decreaseStock()`(벌크쿼리)를 트랜잭션 밖에서 직접
  호출하면 `TransactionRequiredException` → `productOption.stockCount` 직접 감소+save 헬퍼
  (`createPaidOrder`) 사용
- **테스트 공백**(향후 보강 필요, 요청 전엔 먼저 손대지 않기): `AuthService` reissue/rotation,
  `AuthEmailController` login/signup, `OrderShippingService`/`OrderReturnService` 전체, `Product`/
  `Category`/`DeliveryOptionController`(신규 조회 API), `OrderController.getOrders()`(목록 API),
  `CartController`(장바구니 조회/담기/수량변경/삭제), `CouponService`/`PointService`/
  `CouponController`/`PointController`(쿠폰/포인트 신규 기능 전체) — 전부 구현만 하고 테스트 미작성

## 배포

- 로컬: `bootRun --spring.profiles.active=local` (port 16000)
- 운영: 맥미니 자가호스팅(`http://hkh7670.iptime.org:8080`), `dev` 프로파일, nginx 리버스 프록시 뒤
  무중단 배포(`forward-headers-strategy: framework`)
- CORS는 `SecurityConfig.corsConfigurationSource()` 화이트리스트 방식 — 새 프론트엔드/배포 도메인
  추가 시 여기 등록 필요(동일 origin 호출은 CORS 검사 자체가 발동하지 않아 "로컬은 되는데 배포는
  안 됨" 증상의 전형적 원인)

## 알려진 이슈 / 향후 과제

- 카테고리 leaf(소분류) 강제 여부 미정 — 위 "상품/카테고리" 섹션 참고
- 쿠폰/포인트 관리자용 발급 API 미구현(현재는 `seed-data.sql`로만 시딩) — 위 "쿠폰/포인트" 섹션 참고
- 할인 적용 후 최종 결제금액이 0원이 되는 경우(Toss 결제 자체를 생략)는 범위 밖 — 현재는
  `INVALID_DISCOUNT_AMOUNT` 예외로 0원 이하 결제 자체를 차단

## 관련 프로젝트

- `/Users/kyu/workspace/e-commerce-frontend`: 쿠팡 스타일 쇼핑몰 메인 프론트엔드(React+Vite+TS). 홈/상품
  목록·검색/상품상세/장바구니/주문·결제(Toss)/주문내역·취소·반품/로그인(이메일+OAuth) 전체 구현. 백엔드
  API 변경 시 같이 갱신 필요. 자체 CLAUDE.md 참고
- `/Users/kyu/workspace/backend-test-client`: 수동 테스트용 Vite+React+TS 프론트엔드(OAuth 로그인,
  주문, Toss 결제 테스트). 백엔드 API 변경 시 필요하면 같이 갱신
