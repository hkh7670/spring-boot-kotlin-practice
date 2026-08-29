package com.example.springbootkotlinpractice.domain.auth.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class TotpLoginRequest(
    @field:Schema(description = "Email 로그인 시 TOTP 활성화 회원에게 발급된 pending 토큰")
    @field:NotBlank
    val totpPendingToken: String,

    @field:Schema(description = "OTP 앱에 표시된 6자리 코드", example = "123456")
    @field:NotBlank
    val code: String,
)
