package com.example.springbootkotlinpractice.member.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class MemberLoginRequest(
    @Schema(description = "이메일")
    @field:NotBlank
    val email: String,

    @Schema(description = "비밀번호")
    @field:NotBlank
    val password: String,
)
