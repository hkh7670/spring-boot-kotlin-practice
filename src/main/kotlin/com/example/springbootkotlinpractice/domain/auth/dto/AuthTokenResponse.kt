package com.example.springbootkotlinpractice.domain.auth.dto

data class AuthTokenResponse(
    val grantType: String = "Bearer",
    val accessToken: String,
    val refreshToken: String,
)
