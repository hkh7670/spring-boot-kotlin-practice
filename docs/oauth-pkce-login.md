# OAuth (Google / Kakao / Naver) PKCE 로그인 & 회원가입

Google, Kakao, Naver 세 Provider 모두 **Authorization Code + PKCE** 방식으로 통일되어 있다.
클라이언트가 access token 을 직접 서버로 넘기던 기존 Kakao/Naver 방식은 폐기되었다 (2026-07-13).

## 왜 PKCE 인가

- `code_verifier` 없이는 탈취된 `code` 만으로 토큰 교환이 불가능하다.
- 세 Provider의 로그인 처리 로직/DTO/에러 코드가 완전히 동일해져 컨트롤러·서비스 중복이 사라진다.

## 전체 흐름

1. 클라이언트가 PKCE `code_verifier` / `code_challenge` 를 생성한다.
2. 클라이언트가 각 Provider의 `/authorize` 엔드포인트로 이동해 로그인 후 `code` 를 발급받는다.
3. 클라이언트가 `code`, `code_verifier`, `redirect_uri` 를 우리 서버로 전달한다 (아래 API).
4. 서버가 Provider의 토큰 엔드포인트에서 access token 을 교환하고, 이어서 사용자 정보를 조회한다.
5. 서버가 `providerId` 로 기존 회원 여부를 판단해 `LOGIN`(JWT 발급) 또는 `NEED_SIGN_UP`(tempToken 발급) 을 응답한다.
6. `NEED_SIGN_UP` 인 경우 클라이언트는 `/sign-up` 으로 tempToken + 추가 정보(이름/나이/전화번호)를 보내 가입을 완료한다.

## API

Base: `/api/v1/members/oauth`

| Method | Path | 설명 |
|---|---|---|
| POST | `/google` | Google PKCE 로그인 |
| POST | `/kakao` | Kakao PKCE 로그인 |
| POST | `/naver` | Naver PKCE 로그인 |
| POST | `/sign-up` | tempToken 으로 신규 회원가입 |

### 로그인 요청 (`/google`, `/kakao`, `/naver` 공통)

`OAuthAuthorizationCodeLoginRequest`

```json
{
  "code": "4/0AY0e-g7abcDEFghijKLmnoPQRstuv",
  "codeVerifier": "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk",
  "redirectUri": "com.example.app:/oauth2redirect"
}
```

### 로그인 응답

`OAuthLoginResponse`

```json
// 기존 회원
{ "status": "LOGIN", "accessToken": "...", "refreshToken": "..." }

// 신규 회원
{ "status": "NEED_SIGN_UP", "tempToken": "..." }
```

### 회원가입 요청 (`/sign-up`)

`OAuthSignUpRequest`

```json
{
  "tempToken": "eyJhbGciOiJIUzI1NiJ9...",
  "lastName": "한",
  "firstName": "규호",
  "birthDate": "1998-05-20",
  "phoneNumber": "010-1234-5678"
}
```

### 에러 코드

| HTTP | code | 상황 |
|---|---|---|
| 400 | `8001` BAD_REQUEST | 지원하지 않는 provider |
| 400 | `8000` SCHEMA_VALIDATE_ERROR | code/codeVerifier/redirectUri 검증 실패 |
| 401 | `4000` INVALID_JWT_TOKEN | tempToken 이 유효하지 않거나 만료됨 |
| 401 | `4002` INVALID_OAUTH_TOKEN | Provider 에 code/PKCE 검증 실패 (`401` 응답 매핑) |
| 409 | `4005` ALREADY_REGISTERED_OAUTH | 이미 가입된 providerId 로 재가입 시도 |
| 500 | `9999` EXTERNAL_SERVER_ERROR | Provider API 호출 실패 (그 외 4xx/5xx) |

## 아키텍처

```
MemberOAuthController
    └─ MemberAuthService.oauthLoginWithAuthorizationCode(provider, code, codeVerifier, redirectUri)
            └─ OAuthClientResolver.resolve(provider)
                    ├─ GoogleOAuthClient
                    ├─ KakaoOAuthClient
                    └─ NaverOAuthClient
                            └─ OAuthClient.getUserInfoByAuthorizationCode(...)
                                    1) {Provider}TokenApi.exchangeToken(...)  → access token 교환
                                    2) {Provider}OAuthApi.getUserInfo(...)    → 사용자 정보 조회
```

- `OAuthClient` 인터페이스는 provider 별로 다르던 로그인 방식을 `getUserInfoByAuthorizationCode` 하나로 통일한다.
- 토큰 교환용 REST 클라이언트(`GoogleTokenApi`/`KakaoTokenApi`/`NaverTokenApi`)는 사용자 정보 조회용 클라이언트와 호스트가 다르므로 별도 빈으로 분리되어 있다 (`OAuthRestClientConfig`).

| Provider | userinfo host | token host |
|---|---|---|
| Google | `www.googleapis.com` | `oauth2.googleapis.com` |
| Kakao | `kapi.kakao.com` | `kauth.kakao.com` |
| Naver | `openapi.naver.com` | `nid.naver.com` |

## 설정 (application-*.yml)

```yaml
oauth:
  google:
    client-id: ...
    client-secret: ...
  kakao:
    client-id: ...
    client-secret: ...      # 카카오 콘솔에서 "Client Secret" 보안 기능을 켠 경우에만 필요 (nullable)
  naver:
    client-id: ...
    client-secret: ...      # Naver 는 필수
```

`dev`/`local` 프로필에는 `CHANGE_ME_*` 플레이스홀더가 들어 있으므로, 배포 전 각 Provider 콘솔에서 발급받은 실제 값으로 교체해야 한다.

## 주요 파일

- `common/oauth/OAuthClient.kt` — 공용 인터페이스
- `common/oauth/{Google,Kakao,Naver}OAuthClient.kt` — provider 별 PKCE 토큰 교환 + 사용자 정보 조회
- `common/oauth/{Google,Kakao,Naver}TokenApi.kt` — 토큰 교환 전용 REST 클라이언트
- `common/oauth/{Google,Kakao,Naver}OAuthApi.kt` — 사용자 정보 조회 전용 REST 클라이언트
- `common/config/{Google,Kakao,Naver}OAuthProperties.kt` — client-id/secret 바인딩
- `common/config/OAuthRestClientConfig.kt` — RestClient 빈 등록 (host 별 분리, 커넥션 풀 공유)
- `member/api/MemberOAuthController.kt` — API 엔드포인트
- `member/service/MemberAuthService.kt` — 로그인/가입 분기 로직
- `member/dto/OAuthAuthorizationCodeLoginRequest.kt`, `OAuthLoginResponse.kt`, `OAuthSignUpRequest.kt`
- `test/.../member/api/MemberOAuthControllerTest.kt` — 3개 provider 공통 통합 테스트 (14 케이스)

## 참고: Naver PKCE 지원 여부

Naver 공식 문서 기준으로 PKCE(`code_verifier`) 파라미터 지원 여부는 시점에 따라 달라질 수 있다.
현재 구현은 Google/Kakao 와 동일한 요청 바디 형태(`code_verifier` + `client_secret` 동시 전송)로 맞춰뒀으며,
실제 Naver 앱 등록 상태에 따라 토큰 교환이 거부되면 `code_verifier` 파라미터를 제거하고 `client_secret` 만으로
동작하는지 별도 확인이 필요하다.
