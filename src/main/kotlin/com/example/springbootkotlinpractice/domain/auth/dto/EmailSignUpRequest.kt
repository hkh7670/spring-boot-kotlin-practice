package com.example.springbootkotlinpractice.domain.auth.dto

import com.example.springbootkotlinpractice.common.validation.PasswordPolicy
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.*
import java.time.LocalDate

data class EmailSignUpRequest(
    @field:Schema(description = "이름")
    @field:NotBlank
    val firstName: String,

    @field:Schema(description = "성")
    @field:NotBlank
    val lastName: String,

    @field:Schema(description = "생년월일 (yyyy-MM-dd)")
    @field:NotNull
    @field:Past
    val birthDate: LocalDate,

    @field:Schema(description = "휴대전화번호")
    @field:NotBlank
    val phoneNumber: String,

    @field:Schema(description = "이메일 주소")
    @field:NotBlank
    @field:Email
    val email: String,

    @field:Schema(description = "비밀번호 (영문/숫자/특수문자 포함 10~64자)")
    @field:NotBlank
    @field:Pattern(regexp = PasswordPolicy.PATTERN, message = PasswordPolicy.MESSAGE)
    val password: String,
) {
    fun validate() {

    }

}
