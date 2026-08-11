package com.example.springbootkotlinpractice.common.oauth

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.core.user.OAuth2User

// Kakao/Naver 로그인 결과를 감싸는 principal. attributes는 각 Provider의 원본 응답 형태를 그대로 보존한다
class CustomOAuth2User(
    override val oAuthUserInfo: OAuthUserInfo,
    private val attributes: Map<String, Any>,
    private val nameAttributeKey: String,
) : OAuth2User, OAuthPrincipal {
    override fun getName(): String = attributes[nameAttributeKey].toString()
    override fun getAttributes(): Map<String, Any> = attributes
    override fun getAuthorities(): Collection<GrantedAuthority> = emptyList()
}
