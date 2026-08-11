package com.example.springbootkotlinpractice.common.oauth

import org.springframework.security.oauth2.core.oidc.OidcIdToken
import org.springframework.security.oauth2.core.oidc.OidcUserInfo
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser

// Google(OIDC) 로그인 결과를 감싸는 principal.
// userInfo는 null일 수 있다 — UserInfo 엔드포인트를 호출하지 않고 ID 토큰 클레임만으로 충분하다고
// Spring Security가 판단하면 OidcUserService.loadUser()가 userInfo=null인 OidcUser를 반환한다.
// 이 경우 ID 토큰의 클레임으로 OidcUserInfo를 직접 만들어 채운다.
class CustomOidcUser(
    override val oAuthUserInfo: OAuthUserInfo,
    idToken: OidcIdToken,
    userInfo: OidcUserInfo?,
) : DefaultOidcUser(emptyList(), idToken, userInfo ?: OidcUserInfo(idToken.claims)), OAuthPrincipal
