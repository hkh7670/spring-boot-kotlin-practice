package com.example.springbootkotlinpractice.domain.auth.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class OAuthAuthorizationCodeLoginRequest(
    @field:Schema(
        description = "OAuth Provider(Google/Kakao/Naver) Authorization Code. redirect_uri 로 리다이렉트될 때 쿼리스트링(code=)으로 전달받은 값",
        example = "4/0AY0e-g7abcDEFghijKLmnoPQRstuv",
    )
    @field:NotBlank
    val code: String,

    @field:Schema(
        description = "클라이언트가 /authorize 요청 전에 생성해 보관하고 있던 PKCE code_verifier",
        example = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk",
    )
    @field:NotBlank
    val codeVerifier: String,

    @field:Schema(
        description = "OAuth Provider 콘솔에 등록된 redirect URI. /authorize 요청 시 사용한 값과 정확히 일치해야 한다",
        example = "com.example.app:/oauth2redirect",
    )
    @field:NotBlank
    val redirectUri: String,
)
