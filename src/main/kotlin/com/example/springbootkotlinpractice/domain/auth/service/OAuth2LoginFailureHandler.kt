package com.example.springbootkotlinpractice.domain.auth.service

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder

// oauth2Login 실패(취소, 권한 거부, Provider 오류 등) 시 프론트 에러 페이지로 리다이렉트한다
@Component
class OAuth2LoginFailureHandler(
    @Value($$"${app.oauth.frontend-failure-redirect-uri}")
    private val frontendFailureRedirectUri: String,
) : AuthenticationFailureHandler {

    override fun onAuthenticationFailure(
        request: HttpServletRequest,
        response: HttpServletResponse,
        exception: AuthenticationException,
    ) {
        val redirectUri = UriComponentsBuilder.fromUriString(frontendFailureRedirectUri)
            .queryParam("error", exception.message ?: "oauth_login_failed")
            .build()
            .toUriString()
        response.sendRedirect(redirectUri)
    }
}
