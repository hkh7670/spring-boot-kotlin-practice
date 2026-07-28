# 작업 인계 노트 (2026-07-28 기준)

다른 터미널/세션에서 이어서 작업할 수 있도록 이번 세션에서 진행한 내용과 남은 작업을 정리한다.

## 1. Toss 결제 금액 검증 — 쿠폰/포인트 논의 (설계 논의만, 코드 변경 없음)

- 쿠폰/네이버페이 포인트 사용 시 `PaymentService.validateAmount()`에서 실제 결제금액과 스냅샷 금액이
  달라 mismatch가 날 수 있다는 문제 제기.
- 결론: 할인은 **주문 생성 시점에 서버가 확정**해야 한다 (`OrderService.createOrder`가 쿠폰/포인트를
  검증·차감하고 `OrderInfo`류 엔티티에 최종 결제금액을 스냅샷으로 저장). `PaymentService`의
  "요청 금액 == Toss 승인 금액 == 서버 확정 금액" 3자 일치 검증은 그대로 유지하고 느슨하게 풀지 않는다.
- **아직 구현되지 않음** — 쿠폰/포인트 도메인 자체가 아직 없다. 필요해지면 이 설계 방향으로 진행.

## 2. DB 스키마 전면 리네이밍 (완료)

사용자가 `ddl.sql`의 테이블/컬럼/제약조건명을 직접 수정 → 코드도 그에 맞춰 동기화.

| 이전 | 이후 |
|---|---|
| `order_info` / `OrderInfo` | `orders` / `Order` |
| `order_detail_info` / `OrderDetailInfo` | `order_items` / `OrderItem` |
| `order_status_history` / `OrderStatusHistory` | `order_status_histories` / `OrderStatusHistory` (클래스명 유지) |
| `pay_info` / `PayInfo` | `payments` / `Payment` |
| `product_info` / `ProductInfo` | `products` / `Product` |
| `delivery_info` / `DeliveryInfo` | `delivery_options` / `DeliveryOption` |
| `member` / `Member` | `members` / `Member` (클래스명 유지) |

- 엔티티 클래스명 규칙: **테이블명에서 복수형 `s`만 뺀 이름**으로 통일.
- API 필드도 함께 변경: `OrderCreateRequest.deliveryInfoId` → `deliveryOptionId`,
  `PaymentConfirmResponse.payInfoId` → `paymentId`.
- `CLAUDE.md`의 엔티티명 예시, FK 네이밍 컨벤션, 가격 스냅샷 설명 등도 새 이름으로 갱신 완료.
- `mise exec -- ./gradlew compileKotlin compileTestKotlin test` 전체 통과 확인.

## 3. 프론트엔드 동기화 + 프로젝트 리네이밍 (완료)

- `$HOME/workspace/oauth-test` → **`$HOME/workspace/backend-test-client`**로 디렉토리/프로젝트명 변경
  (더 이상 OAuth 테스트 전용이 아니라 백엔드 전반의 수동 테스트 클라이언트라서).
- `deliveryInfoId` → `deliveryOptionId` (`OrderTest.tsx` 상태 변수/요청 필드) 반영.
- `package.json`/`package-lock.json`의 `name` 필드, 하드코딩된 Toss `orderName` 문자열,
  백엔드 `CLAUDE.md`의 "관련 프로젝트" 참조 경로까지 전부 갱신.

## 4. 상품 카테고리(대/중/소분류) + 업체 정보 스키마 추가 (구현 완료, 미해결 이슈 있음)

**확정된 설계:**
- `categories` 테이블: **단일 테이블 + `parent_id`(self-referencing) + `level`(`LARGE`/`MEDIUM`/`SMALL`
  enum)** 계층 구조. 대/중/소분류를 각각 별도 테이블로 만들지 않기로 함.
- `vendors` 테이블: 업체(공급사) 정보. **상품 1개당 업체 1개** (`products.vendor_id`, 1:N).
- `products.category_id` / `products.vendor_id`: nullable, **FK 제약 없음** (raw `Long?` 필드 —
  `Payment`/`OrderStatusHistory`와 동일하게 `@ManyToOne` 관계 대신 순수 ID 필드만 사용).

**구현된 파일:**
- `enums/CategoryLevel.kt`
- `domain/category/entity/Category.kt`, `domain/category/repository/CategoryRepository.kt`
- `domain/vendor/entity/Vendor.kt`, `domain/vendor/repository/VendorRepository.kt`
- `domain/product/entity/Product.kt`에 `categoryId`/`vendorId` 필드 추가
- `ddl.sql`에 `categories`/`vendors` 테이블 신규 추가, `products`에 `category_id`/`vendor_id`
  컬럼 + 인덱스(`idx_products_01`, `idx_products_02`) 추가

**⚠️ 미해결 — 다음 세션에서 결정 필요:**
"상품의 카테고리가 항상 소분류(leaf)여야 하는가?"라는 질문이 나왔고, 쿠팡처럼 대/중분류에서
더 세분화되지 않는 경우도 있을 수 있다는 논의를 했다. 결론:
- 현재 스키마는 DB 레벨에서 리프(소분류) 강제를 **전혀 하지 않는다** — `category_id`는 그냥
  `categories.id`를 가리키는 FK라 대/중/소 어느 레벨이든 넣을 수 있다.
- 다만 `Product.kt`의 `@Comment`와 `ddl.sql`의 컬럼 주석에는 `"카테고리 ID (categories.id, 소분류)"`
  라고 적혀 있어서, 마치 리프 전용인 것처럼 잘못 문서화돼 있다.
- **다음에 할 일**: 이 주석/문서 표현을 "대/중/소분류 중 어느 레벨이든 가능"하다는 뉘앙스로 고칠지,
  아니면 다른 추가 조치(예: 서비스 레이어에서의 검증 로직 등)가 필요한지 사용자와 논의해서 결정할 것.
  (`Product.kt`의 두 `@Comment` 문구, `ddl.sql`의 `products.category_id` 컬럼 주석이 대상)

## 5. OAuth PKCE 관련 확인 (코드 변경 없음, 질문에 답변만)

- Kakao/Naver의 `OAuthClient.getUserInfoByAuthorizationCode()`는 `codeVerifier` 파라미터를 받지만
  실제로 `TokenRequest`를 만들 때 사용하지 않는다 (`client_secret`만 사용). 의도된 설계이며
  `KakaoOAuthClient.kt`/`NaverOAuthClient.kt`에 주석으로도 명시돼 있다. 코드 변경 없음.
- `client_secret`은 `@ConfigurationProperties`로 백엔드(`.env`)에만 존재하고 프론트에 노출되지 않음.
- `code`는 OAuth 제공자가 로그인 성공 후 리다이렉트 시 부여하는 단기 유효(제공자마다 다르지만 보통
  10분 이내)·1회용 인가 코드임을 확인 (`Callback.tsx`의 `hasExchangedRef` 가드가 이 1회성 때문에 존재).

## 6. `backend-test-client` Git 저장소화 (부분 완료)

- `.gitignore` 추가 (`node_modules`, `dist`, `*.tsbuildinfo`, `.env` 등).
- `git init` + 최초 커밋 완료 (커밋 `b107591`, `main` 브랜치, 19개 파일).
- **GitHub 원격 연결/푸시는 아직 안 함** — 이 머신에 `gh` CLI가 설치되어 있지 않아서 자동으로
  저장소를 만들 수 없었고, 사용자가 "일단 로컬 초기화만" 하기로 선택함.
- **다음에 할 일**: 사용자가 GitHub에서 직접 빈 저장소를 만들고 URL을 주면
  `git remote add origin <url>` + `git push -u origin main`으로 이어서 진행.
  (또는 `gh` CLI를 설치/로그인한 뒤 `gh repo create`로 자동 생성도 가능.)

## 프로젝트 경로

- 백엔드: `/Users/kyu/workspace/spring-boot-kotlin-practice`
- 프론트(수동 테스트 클라이언트, 구 `oauth-test`): `/Users/kyu/workspace/backend-test-client`
  (독립 git 저장소, 아직 origin 없음)

## 다음 세션 우선순위 TODO

1. 상품 카테고리 leaf-only 여부 관련 주석/문서 정리 (섹션 4 참고)
2. `backend-test-client` GitHub 원격 연결 + 푸시
3. (필요 시) 쿠폰/포인트 도메인 설계 착수 (섹션 1 참고, 아직 미착수)
