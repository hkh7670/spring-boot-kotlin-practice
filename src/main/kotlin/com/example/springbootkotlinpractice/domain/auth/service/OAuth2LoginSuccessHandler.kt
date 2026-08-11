package com.example.springbootkotlinpractice.domain.auth.service

import com.example.springbootkotlinpractice.common.oauth.OAuthPrincipal
import com.example.springbootkotlinpractice.enums.JoinProvider
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder

// oauth2Login 성공 시 LOGIN/NEED_SIGN_UP 을 판단해 relay code를 발급하고 프론트로 리다이렉트한다
@Component
class OAuth2LoginSuccessHandler(
    private val authService: AuthService,
    private val oAuthRelayCodeService: OAuthRelayCodeService,
    @Value($$"${app.oauth.frontend-success-redirect-uri}")
    private val frontendSuccessRedirectUri: String,
) : AuthenticationSuccessHandler {

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication,
    ) {
        val oAuth2Token = authentication as OAuth2AuthenticationToken
        val provider = JoinProvider.valueOf(oAuth2Token.authorizedClientRegistrationId.uppercase())
        val oAuthUserInfo = (oAuth2Token.principal as OAuthPrincipal).oAuthUserInfo

        val loginResponse = authService.oauthLogin(provider, oAuthUserInfo)
        val code = oAuthRelayCodeService.issue(loginResponse)

        val redirectUri = UriComponentsBuilder.fromUriString(frontendSuccessRedirectUri)
            .queryParam("code", code)
            .build()
            .toUriString()
        response.sendRedirect(redirectUri)
    }
}
