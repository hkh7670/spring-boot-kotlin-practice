# spring-boot-kotlin-practice

Kotlin + Spring Boot 3 학습/실습용 프로젝트. OAuth 로그인, JWT 인증, 주문/결제(Toss Payments) 도메인을
직접 구현하며 Spring 생태계를 익히는 것이 목적이다.

## 기술 스택

- Kotlin 2.4.10 / Java 21 toolchain (Gradle Kotlin DSL, `kotlin("plugin.spring")`, `kotlin("plugin.jpa")`, `kapt`)
- Spring Boot 3.5.16 (`spring-boot-starter-web`, `-data-jpa`, `-security`, `-validation`, `-data-redis`)
- DB: MySQL(운영/dev), H2(local/test) — QueryDSL(OpenFeign jakarta 포크, `querydsl-jpa` 7.4.0)
- 인증: JWT(`jjwt` 0.13.x, HMAC) + Redis(refresh token rotation) + Spring Security
- 외부 연동: Google/Kakao/Naver OAuth(PKCE), Toss Payments
- API 문서: springdoc-openapi (`/swagger.html`, `/api-docs`)
- ULID(`ulid-creator`) — 외부 노출용 식별자(`orderUid` 등)

## 빌드 / 실행

**중요: 이 프로젝트는 `mise.toml`로 `java = temurin-21`을 고정한다. 에이전트 Bash 세션은 mise가 자동
활성화되지 않을 수 있으므로, Gradle 명령은 항상 `mise exec --`를 앞에 붙여 실행한다.**

```bash
mise exec -- ./gradlew compileKotlin compileTestKotlin   # 컴파일만
mise exec -- ./gradlew test                              # 전체 테스트 (H2, profile=test)
mise exec -- ./gradlew bootRun --args='--spring.profiles.active=local'
```

이걸 생략하면 앰비언트 JDK(예: temurin-25)와 버전이 어긋나 `kotlin("plugin.spring")` 컴파일러가
`JavaVersion.parse` 단계에서 크래시한다 (`IllegalArgumentException: 25.0.3` 형태의 에러).

### 프로파일 (`src/main/resources/application-*.yml`)

| 프로파일 | 용도 | DB | `ddl-auto` |
|---|---|---|---|
| `local` | 로컬 개발 | 외부 MySQL (`.env`) | `none` (수동 DDL) |
| `dev` | 무중단 배포 서버 (맥미니, `hkh7670.iptime.org:8080`) | 외부 MySQL | `none` |
| `mysql` | MySQL 전용 설정 조각 (local/dev가 참조) | 외부 MySQL | `none` |
| `h2` | 로컬 인메모리 실행 | H2 (`MODE=MySQL`) | `create-drop` |
| `test` | 테스트 실행 (`src/test/resources/application-test.yml`) | H2 인메모리 | `create-drop` |

`local`/`dev`/`mysql` 프로파일은 `ddl-auto: none`이라 스키마 변경 시 **`ddl.sql`을 사람이 직접 실행**해야
한다 (아래 "DB 스키마" 참고). `h2`/`test`는 엔티티로부터 매 실행마다 스키마를 자동 생성한다.

민감정보는 `.env`에 있고 gitignore 되어 있다 (DB 접속정보, OAuth client secret, Redis 비밀번호,
Toss 키, JWT/AES 시크릿). `.env`에는 각 키를 어떻게 생성했는지 명령어 주석이 달려있다
(`openssl rand -base64 ...`).

## 패키지 구조

```
common/            도메인에 종속되지 않는 공통 코드
  config/          @ConfigurationProperties, SecurityConfig, SwaggerConfig, Redis/OAuth/Toss 설정
  security/        JwtTokenProvider, JwtAuthenticationFilter, UserPrincipal
  oauth/{google,kakao,naver}/  Provider별 OAuthClient + TokenApi + OAuthApi (Feign 스타일 인터페이스)
  redis/           RedisRepository (StringRedisTemplate 얇은 래퍼)
  converter/       Aes256Converter (JPA @Convert, PII 컬럼 암호화)
  utils/           AesCryptoUtil
  dto/             CommonResponse, ResponseHandler (API 공통 응답 포맷)
  entity/          BaseTimeEntity (createdDatetime/updatedDatetime, @EntityListeners auditing)
domain/
  auth/            로그인/회원가입/토큰 재발급 (EMAIL, OAuth 공통)
  member/          회원 조회
  order/           주문 생성/조회, 주문 상태 이력, 만료 주문 취소 배치
  payment/         Toss Payments 결제 승인
  product/         상품 (엔티티/레포지토리만, 컨트롤러 없음 — 시딩은 H2 콘솔로)
  delivery/        배송 옵션 (엔티티/레포지토리만)
enums/             전역 enum (ResponseCodeEnum, OrderStatus, PaymentStatus, JoinProvider, Role, TokenType)
exception/         ApiErrorException, ApiCommonAdvice(@RestControllerAdvice), ErrorInfo
```

각 도메인 패키지는 `api/ dto/ entity/ repository/ service/` 하위 구조를 따른다 (있는 것만).
Controller → Service → Repository 레이어를 엄격히 지키고, Entity를 API 응답에 직접 노출하지 않는다
(항상 DTO로 변환).

## 공통 컨벤션

- **엔티티 생성은 companion object의 `of()` / `ofOAuth()` 팩토리 메서드**를 통해서만 한다 (public
  생성자를 직접 호출하지 않음). 예: `Member.of(...)`, `Order.of(...)`, `Payment.of(...)`.
- **에러는 `ApiErrorException(ResponseCodeEnum.XXX)`로 던진다.** 도메인 검증 실패를 `check()`/`require()`로
  처리하지 않는다 — `ApiCommonAdvice`가 `ResponseCodeEnum`의 `httpStatus`/`resultCode`/`resultMsg`를
  일관된 `CommonResponse` 포맷으로 변환해준다.
- **PII(이름/전화번호/이메일)는 `@Convert(converter = Aes256Converter::class)`로 저장 시 자동 암호화된다.**
  IV가 고정이라 결정적(deterministic) 암호화이며, 이는 암호화된 컬럼으로 `WHERE` 동등 조회(로그인 등)를
  하기 위한 의도된 트레이드오프다 — "취약점"으로 보고 임의로 랜덤 IV로 바꾸지 말 것.
  비밀번호(`Member.password`)는 AES가 아니라 BCrypt 해시로 별도 저장한다.
- **모든 엔티티는 `BaseTimeEntity`를 상속**해 `createdDatetime`/`updatedDatetime`을 자동 관리한다
  (`@EnableJpaAuditing` 필요, 메인 애플리케이션 클래스에 설정됨).
- **FK 성격의 컬럼/필드명은 참조 테이블명(단수형) + `_id`** 컨벤션을 따른다 (`order_id`,
  `product_id`, `delivery_option_id`, `member_id`).
- **Spring Data 파생 쿼리 메서드명은 엔티티의 Kotlin 프로퍼티명과 정확히 일치해야 한다** (DB 컬럼명이
  아니라). 예를 들어 `OrderItem`은 `@ManyToOne val order: Order`(컬럼명 `order_id`) 관계를 가지므로
  파생 쿼리는 `findByOrderId`(중첩 프로퍼티 경로 `order.id`)로 작성한다 — 프로퍼티 경로와 다르게 쓰면
  `PropertyReferenceException`으로 Spring 컨텍스트 자체가 기동 실패하고, 컨텍스트를 공유하는 모든
  `@SpringBootTest` 테스트가 도미노로 실패한다.
- 컬렉션/문자열 null-or-empty 체크는 `CollectionUtils.isEmpty()`/`StringUtils.hasText()` 사용(전역
  CLAUDE.md 규칙), `!!` 사용 금지.

## 인증/인가 (`domain/auth`, `common/security`)

- **가입 경로 2가지**: EMAIL(비밀번호, BCrypt) / OAuth(GOOGLE·KAKAO·NAVER, PKCE). 회원 조회 유니크
  제약은 `(provider_id, join_provider)`와 `(email, join_provider)` 조합이라, OAuth와 EMAIL로 같은
  이메일을 각각 가입할 수 있다.
- **OAuth는 세 Provider 모두 Authorization Code + PKCE로 통일**되어 있다 (Kakao/Naver는 PKCE 미지원이라
  대신 서버가 보관하는 `client_secret`으로 보호). 흐름은 `docs/oauth-pkce-login.md` 참고 — 단, 그 문서는
  구 패키지 구조(`member` 도메인) 기준으로 쓰여져 API 베이스 경로가 `/api/v1/members/oauth`로 남아있다.
  **실제 현재 경로는 `/api/v1/auth/oauth`** (도메인 재구성 이후 변경됨, 문서 미반영 상태).
- **토큰 3종류** (`TokenType`): `ACCESS_TOKEN`(30분), `REFRESH_TOKEN`(14일), `TEMP_TOKEN`(10분, OAuth
  신규가입 중간 단계). `JwtAuthenticationFilter`는 **`ACCESS_TOKEN` 타입일 때만** 인증 컨텍스트를
  채운다 — REFRESH/TEMP 토큰을 Bearer로 잘못 보내면 그냥 인증되지 않은 요청으로 처리된다(500이 아님).
- **Refresh Token Rotation**: `AuthService.issue()`가 발급할 때마다 Redis(`refresh-token:{memberId}`,
  TTL=refreshTokenValidityMs)에 최신 토큰을 덮어쓴다. `reissue()`는 요청받은 refreshToken이 Redis에
  저장된 것과 다르면 이미 폐기된 토큰의 재사용(탈취 의심)으로 간주해 Redis 키를 삭제하고 401을 던진다
  (강제 로그아웃). 회원당 세션 1개만 유지되는 모델이다.
- JWT 서명키는 `jwtProperties.secret`을 **UTF-8 바이트 그대로** 사용한다 (AES와 달리 base64 디코딩
  안 함). `AesCryptoUtil`은 반대로 `Base64.getDecoder().decode(...)`를 거친다 — 둘을 혼동하지 말 것.

## 주문/결제 (`domain/order`, `domain/payment`)

- **재고 차감은 조건부 UPDATE로 원자적으로 처리** (`ProductRepository.decreaseStock`,
  `WHERE stock_count >= :count`) — 동시 주문에 의한 초과 판매 방지. 영향받은 row가 0이면
  `NOT_ENOUGH_STOCK`.
- **가격 스냅샷**: `OrderItem.price`(상품 가격), `Order.deliveryPrice`(배송비)는 주문
  시점 값을 스냅샷으로 저장한다. `products.price`/`delivery_options.price`는 이후 관리자가 바꿀 수
  있으므로, 주문 조회/결제 금액 검증 시 **절대 라이브 조회값을 다시 계산에 쓰지 않는다** — 반드시
  `orders`/`order_items`에 저장된 스냅샷을 사용한다.
- **주문 상태 이력**: `OrderStatusHistory`가 `Order.status`가 바뀔 때마다(생성 시
  `PENDING_PAYMENT`, 결제완료 `PAID`, 취소 `CANCELLED`) append-only로 쌓인다. FK 제약은 의도적으로
  걸지 않았다.
- **만료 주문 자동 취소**: `StaleOrderCancelScheduler`가 10분마다(`@Scheduled(cron = "0 */10 * * * *")`)
  생성된 지 10분 넘은 `PENDING_PAYMENT` 주문을 찾아 취소 + 재고 복구한다 (`@EnableScheduling` 필요,
  메인 애플리케이션 클래스에 설정됨).
- **결제(Toss) 확정은 외부 API 호출과 DB 쓰기를 서비스 단위로 분리**했다:
  - `PaymentService`: 검증(소유자/상태/금액) → Toss API 호출 → 결과에 따라 `PaymentRecordService`
    위임. `@Transactional`을 걸지 않는다 — 트랜잭션 안에서 느릴 수 있는 외부 HTTP 호출을 하면 DB
    커넥션을 오래 붙잡아 커넥션 풀 고갈 위험이 있기 때문.
  - `PaymentRecordService`: `completePayment()`/`cancelOrderAndRestoreStock()`가 각각 별도
    `@Transactional`. **반드시 별도 Spring 빈으로 분리되어 있어야** `PaymentService`에서 호출할 때
    프록시를 통과해 트랜잭션이 실제로 적용된다 (Kotlin `kotlin("plugin.spring")`이 `@Service` 클래스를
    자동으로 open 처리해 CGLIB 프록시가 가능해짐). 같은 클래스 내부 self-invocation으로 합치면
    `@Transactional`이 조용히 무시된다 — 절대 두 서비스를 하나로 합치지 말 것.
  - `cancelOrderAndRestoreStock()`은 결제 실패 취소와 만료 주문 배치 취소 양쪽에서 재사용된다.

## DB 스키마 (`ddl.sql`)

프로젝트 루트의 `ddl.sql`이 스키마의 단일 소스다. 두 부분으로 구성:
1. 파일 앞부분: 전체 `CREATE TABLE` (신규 DB 구축용, 최신 스키마 반영).
2. 파일 끝부분: `-- 기존 DB에 ... 사용` 주석이 붙은 `ALTER TABLE`/`CREATE TABLE` 문 (운영 DB처럼
   이미 존재하는 DB에 변경분만 적용할 때 수동 실행).

**엔티티를 바꿀 때마다 이 두 부분을 모두 갱신해야 한다** — `local`/`dev`/`mysql` 프로파일은
`ddl-auto: none`이라 Hibernate가 스키마를 자동으로 맞춰주지 않는다. (`h2`/`test`만 `create-drop`으로
매번 자동 반영되므로 테스트 통과만으로 배포 DB 반영을 확인했다고 착각하지 말 것.)

## 테스트

- `src/test/kotlin/.../domain/*/api/*ControllerTest.kt`: `MockMvc` + 실제 H2 레포지토리 조합의 통합
  테스트. 외부 API만 `@MockitoBean`으로 목킹한다 (예: `TossPaymentsApi`, OAuth Provider 클라이언트).
  Redis도 테스트 환경에 실제 서버가 없으므로 Redis에 쓰는 서비스(`AuthService` 등)를 거치는 테스트는
  `RedisRepository`를 `@MockitoBean`으로 대체한다.
- 테스트는 `profile=test` (`src/test/resources/application-test.yml`), H2 인메모리 + `create-drop`.
- 커버리지가 얇은 영역(향후 보강 필요, 특별히 요청받기 전에는 먼저 손대지 않기): `AuthService`의
  reissue/rotation 로직, `AuthEmailController`의 login/signup.

## 배포

- 로컬: `mise exec -- ./gradlew bootRun` 또는 `--spring.profiles.active=local` (port 16000).
- 운영: 맥미니 자가 호스팅, `http://hkh7670.iptime.org:8080`, `dev` 프로파일, nginx 리버스 프록시
  뒤에서 무중단 배포. `server.forward-headers-strategy: framework`가 설정되어 있어 프록시 뒤에서도
  스킴/호스트를 올바르게 인식한다.
- **CORS**는 `SecurityConfig.corsConfigurationSource()`의 `allowedOrigins`에 화이트리스트로 등록된
  origin만 허용한다 (`/api/v1/**`에 적용). 새 프론트엔드/배포 도메인을 추가할 때마다 여기에 추가해야
  한다 — 안 하면 브라우저에서 `403 Invalid CORS request`가 발생한다 (동일 origin에서 호출하는 경우,
  예: `localhost:16000`에서 그 자신의 Swagger UI를 호출하는 경우는 CORS 검사 자체가 발동하지 않아
  문제없이 동작한다는 점에 유의 — "왜 로컬은 되는데 배포 도메인은 안 되지" 라는 증상의 전형적 원인).

## 관련 프로젝트

- `/Users/kyu/workspace/oauth-test`: 이 백엔드를 수동 테스트하기 위한 Vite + React + TS 프론트엔드
  (OAuth 로그인, 주문 생성, Toss 결제 테스트 페이지 포함). 백엔드 API 변경 시 필요하면 같이 갱신한다.

## 문서

- `docs/oauth-pkce-login.md`: OAuth PKCE 흐름 설명 (API 베이스 경로는 최신화 필요, 위 참고).
- `docs/order-toss-payment-integration.md`: 주문/Toss 결제 연동 설계 문서.
