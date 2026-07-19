package com.example.springbootkotlinpractice.domain.auth.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class RefreshTokenRequest(
    @field:Schema(description = "로그인 시 발급받은 Refresh Token", example = "eyJhbGciOiJIUzI1NiJ9...")
    @field:NotBlank
    val refreshToken: String,
)
