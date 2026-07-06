package com.example.springbootkotlinpractice.member.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "OAuth 로그인 처리 결과 상태 (LOGIN: 기존 회원 로그인 완료, NEED_SIGN_UP: 신규 회원, 회원가입 필요)")
enum class OAuthLoginStatus {
    LOGIN,
    NEED_SIGN_UP,
}

data class OAuthLoginResponse(
    @field:Schema(description = "로그인 처리 결과 상태")
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
