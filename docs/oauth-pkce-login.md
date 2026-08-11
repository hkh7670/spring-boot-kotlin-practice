# OAuth2 로그인 흐름 (oauth2Login + Relay Code)

Google/Kakao/Naver 로그인이 **백엔드 주도(oauth2Login)**로 시작되고, 토큰은 URL에
직접 노출되지 않도록 **1회용 relay code**를 거쳐 프론트로 전달되는 구조.

## 전체 시퀀스

1. **로그인 시작 (프론트 → 백엔드)**
   `Home.tsx` 버튼 클릭 → `window.location.href = "http://localhost:16000/oauth2/authorization/{provider}"`
   (fetch가 아닌 전체 페이지 이동이라 CORS 영향 없음)

2. **백엔드 → Provider 리다이렉트**
   `OAuth2AuthorizationRequestRedirectFilter`가 요청을 가로채,
   `application-local.yml`에 등록된 `client-id`/`scope`/`redirect-uri`로
   Provider `/authorize` URL을 구성하고 `state` 생성.
   이 인가 요청 상태는 로그인 왕복 구간에만 존재하는 세션(JSESSIONID 쿠키)에 저장.
   브라우저를 Provider 로그인 페이지로 302 리다이렉트.

3. **Provider → 백엔드 콜백**
   로그인/동의 완료 시 Provider가
   `http://localhost:16000/login/oauth2/code/{provider}?code=...&state=...`로 리다이렉트.
   `OAuth2LoginAuthenticationFilter`가 이를 받아:
    - `state` 대조 (CSRF 방지)
    - Provider 토큰 엔드포인트에 `code` 전달 → access token 교환
    - 유저정보 조회:
        - Google → `CustomOidcUserService` (OIDC)
        - Kakao/Naver → `CustomOAuth2UserService` (`kakao_account`/`response` 내부 파싱)
    - 결과를 공통 형태 `OAuthUserInfo(providerId, email, nickname)`로 통일

4. **로그인/가입 분기 + relay code 발급**
   `OAuth2LoginSuccessHandler.onAuthenticationSuccess()`:
    - `AuthService.oauthLogin(provider, userInfo)` 호출
      → `MemberRepository.findByProviderIdAndJoinProvider()`로 기존 회원 여부 확인
        - 기존 회원 → JWT(access+refresh) 발급, `LOGIN` 상태
        - 신규 회원 → `tempToken`(가입용 JWT) 발급, `NEED_SIGN_UP` 상태
    - 결과(`OAuthLoginResponse`)를 `OAuthRelayCodeService.issue()`가
      UUID 키로 **Redis에 60초 TTL** 저장, UUID(`code`) 반환
    - `http://localhost:3000/oauth/complete?code={UUID}`로 리다이렉트
     (실패 시 `OAuth2LoginFailureHandler` → `/oauth/error?error=...`)

5. **프론트: relay code → 실제 결과 교환**
   `OAuthComplete.tsx`가 `code` 쿼리스트링을 읽어 `POST /api/v1/auth/oauth/e
   → `OAuthRelayCodeService.consume()`이 Redis에서 조회 후 **즉시 삭제**(1회용)
   → `OAuthLoginResponse`를 응답 바디로 반환

6. **최종 처리**
    - `LOGIN` → 프론트가 `accessToken`/`refreshToken`을 `localStorage`에 저장
    - `NEED_SIGN_UP` → 회원가입 폼 표시 → 제출 시
      `POST /api/v1/auth/oauth/sign-up` (`tempToken` + 추가정보)
      → `AuthService.oauthSignUp()`이 `Member` 생성 후 JWT 발급

## 핵심 설계 포인트

- **토큰은 URL에 한 번도 노출되지 않음** — URL에 실리는 건 항상 1회용 relay
- **redirect_uri는 항상 백엔드 도메인** (`localhost:16000/login/oauth2/code/{provider}`) —
  Provider는 프론트 도메인을 전혀 모름
- **세션 쿠키는 로그인 왕복 구간에서만 일시적으로 사용** — 이후 API 인증은 여전히
  완전 stateless JWT(Bearer 헤더)

## 관련 파일

| 역할 | 파일 |
|---|---|
| 로그인 시작 | `Home.tsx` |
| Provider 유저정보 매핑 | `CustomOAuth2UserService`, `CustomOidcUserService` |
| 로그인/가입 분기 | `AuthService.oauthLogin()` |
| relay code 발급/소비 | `OAuthRelayCodeService` |
| 콜백 성공/실패 처리 | `OAuth2LoginSuccessHandler`, `OAuth2LoginFailureHand
| relay code 교환 API | `AuthOAuthController.exchange()` |
| 프론트 착지 페이지 | `OAuthComplete.tsx` |