package com.example.springbootkotlinpractice.member.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class OAuthLoginRequest(
    @field:Schema(
        description = "OAuth Provider(Kakao/Naver)로부터 클라이언트가 발급받은 Access Token",
        example = "ya29.a0AfH6SMBx...",
    )
    @field:NotBlank
    val accessToken: String,
)
