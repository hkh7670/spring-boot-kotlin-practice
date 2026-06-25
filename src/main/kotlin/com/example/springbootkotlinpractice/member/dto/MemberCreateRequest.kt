package com.example.springbootkotlinpractice.member.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero

data class MemberCreateRequest(
    @field:NotBlank
    val firstName: String,

    @field:NotBlank
    val lastName: String,

    @field:NotNull
    @field:PositiveOrZero
    val age: Int,

    @field:NotBlank
    val phoneNumber: String,

    @field:NotBlank
    val email: String,
) {

}