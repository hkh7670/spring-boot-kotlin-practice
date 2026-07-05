package com.example.springbootkotlinpractice.member.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero

data class OAuthSignUpRequest(
    @field:NotBlank
    val tempToken: String,

    @field:NotBlank
    val lastName: String,

    @field:NotBlank
    val firstName: String,

    @field:NotNull
    @field:PositiveOrZero
    val age: Int,

    @field:NotBlank
    val phoneNumber: String,
)
