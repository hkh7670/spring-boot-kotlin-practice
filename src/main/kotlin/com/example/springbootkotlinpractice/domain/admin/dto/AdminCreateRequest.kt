package com.example.springbootkotlinpractice.domain.admin.dto

import com.example.springbootkotlinpractice.common.validation.PasswordPolicy
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class AdminCreateRequest(
    @field:Schema(description = "이름")
    @field:NotBlank
    val name: String,

    @field:Schema(description = "이메일 주소")
    @field:NotBlank
    @field:Email
    val email: String,

    @field:Schema(description = "비밀번호 (영문/숫자/특수문자 포함 10~64자)")
    @field:NotBlank
    @field:Pattern(regexp = PasswordPolicy.PATTERN, message = PasswordPolicy.MESSAGE)
    val password: String,
)
