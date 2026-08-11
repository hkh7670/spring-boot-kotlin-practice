package com.example.springbootkotlinpractice.domain.auth.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class OAuthExchangeRequest(
    @field:Schema(
        description = "OAuth 로그인 콜백 처리 후 백엔드가 프론트로 리다이렉트할 때 실어준 1회용 relay code",
        example = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    )
    @field:NotBlank
    val code: String,
)
