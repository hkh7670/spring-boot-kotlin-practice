package com.example.springbootkotlinpractice.member.dto

import jakarta.validation.constraints.*
import java.time.LocalDate

data class MemberCreateRequest(
    @field:NotBlank
    val firstName: String,

    @field:NotBlank
    val lastName: String,

    @field:NotNull
    @field:Past
    val birthDate: LocalDate,

    @field:NotBlank
    val phoneNumber: String,

    @field:NotBlank
    @field:Email
    val email: String,

    @field:NotBlank
    @field:Size(min = 8, max = 64)
    val password: String,
) {
    fun validate() {

    }

}