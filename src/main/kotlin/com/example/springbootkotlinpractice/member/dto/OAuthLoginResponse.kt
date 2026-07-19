package com.example.springbootkotlinpractice.member.dto

import com.example.springbootkotlinpractice.member.enums.OAuthLoginStatus
import io.swagger.v3.oas.annotations.media.Schema


data class OAuthLoginResponse(
    @field:Schema(description = OAuthLoginStatus.API_DOCS_DESC)
    val status: OAuthLoginStatus,

    @field:Schema(
        description = "신규 회원(NEED_SIGN_UP)인 경우에만 발급되는 임시 토큰. /sign-up 요청에 사용",
        nullable = true,
    )
    val tempToken: String? = null,

    @field:Schema(description = "기존 회원(LOGIN)인 경우에만 발급되는 Access Token", nullable = true)
    val accessToken: String? = null,

    @field:Schema(description = "기존 회원(LOGIN)인 경우에만 발급되는 Refresh Token", nullable = true)
    val refreshToken: String? = null,
)
