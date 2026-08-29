package com.example.springbootkotlinpractice.domain.auth.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class TotpVerifyRequest(
    @field:Schema(description = "OTP 앱에 표시된 6자리 코드", example = "123456")
    @field:NotBlank
    val code: String,
)
