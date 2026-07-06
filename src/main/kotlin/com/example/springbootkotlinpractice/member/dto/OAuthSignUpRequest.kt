package com.example.springbootkotlinpractice.member.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero

data class OAuthSignUpRequest(
    @field:Schema(description = "OAuth 로그인 시 신규 회원에게 발급된 임시 토큰", example = "eyJhbGciOiJIUzI1NiJ9...")
    @field:NotBlank
    val tempToken: String,

    @field:Schema(description = "성", example = "한")
    @field:NotBlank
    val lastName: String,

    @field:Schema(description = "이름", example = "규호")
    @field:NotBlank
    val firstName: String,

    @field:Schema(description = "나이", example = "28")
    @field:NotNull
    @field:PositiveOrZero
    val age: Int,

    @field:Schema(description = "휴대폰 번호", example = "010-1234-5678")
    @field:NotBlank
    val phoneNumber: String,
)
