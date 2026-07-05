package com.example.springbootkotlinpractice.common.oauth

data class OAuthUserInfo(
    val providerId: String,
    val email: String,
    val nickname: String,
)
