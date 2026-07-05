package com.example.springbootkotlinpractice.member.dto

import jakarta.validation.constraints.NotBlank

data class OAuthLoginRequest(
    @field:NotBlank
    val accessToken: String,
)
