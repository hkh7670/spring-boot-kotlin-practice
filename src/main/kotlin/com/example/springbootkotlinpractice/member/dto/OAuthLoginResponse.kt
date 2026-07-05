package com.example.springbootkotlinpractice.member.dto

enum class OAuthLoginStatus {
    LOGIN,
    NEED_SIGN_UP,
}

data class OAuthLoginResponse(
    val status: OAuthLoginStatus,
    val tempToken: String? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null,
)
