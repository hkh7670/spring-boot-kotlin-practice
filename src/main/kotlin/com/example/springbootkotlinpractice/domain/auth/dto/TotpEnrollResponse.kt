package com.example.springbootkotlinpractice.domain.auth.dto

import io.swagger.v3.oas.annotations.media.Schema

data class TotpEnrollResponse(
    @field:Schema(description = "OTP 앱에 수동 입력할 때 사용하는 시크릿 키(Base32)")
    val secret: String,

    @field:Schema(description = "OTP 앱 QR 코드로 렌더링할 otpauth:// URI")
    val otpAuthUri: String,
)
