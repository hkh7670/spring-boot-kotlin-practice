package com.example.springbootkotlinpractice.enums

enum class TokenType(
    val desc: String,
) {
    ACCESS_TOKEN("Access Token"),
    REFRESH_TOKEN("Refresh Token"),
}