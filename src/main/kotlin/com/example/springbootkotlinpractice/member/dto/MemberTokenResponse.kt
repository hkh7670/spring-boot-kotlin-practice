package com.example.springbootkotlinpractice.member.dto

data class MemberTokenResponse(
    val grantType: String = "Bearer",
    val accessToken: String,
    val refreshToken: String,
)