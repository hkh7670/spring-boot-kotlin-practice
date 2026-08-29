package com.example.springbootkotlinpractice.domain.auth.dto

import com.example.springbootkotlinpractice.domain.auth.enums.EmailLoginStatus
import io.swagger.v3.oas.annotations.media.Schema

data class EmailLoginResponse(
    @field:Schema(description = EmailLoginStatus.API_DOCS_DESC)
    val status: EmailLoginStatus,

    @field:Schema(
        description = "TOTP 인증 필요(NEED_TOTP)인 경우에만 발급되는 pending 토큰. /totp/login 요청에 사용",
        nullable = true,
    )
    val totpPendingToken: String? = null,

    @field:Schema(description = "TOTP 미사용 회원(LOGIN)인 경우에만 발급되는 Access Token", nullable = true)
    val accessToken: String? = null,

    @field:Schema(description = "TOTP 미사용 회원(LOGIN)인 경우에만 발급되는 Refresh Token", nullable = true)
    val refreshToken: String? = null,
)
