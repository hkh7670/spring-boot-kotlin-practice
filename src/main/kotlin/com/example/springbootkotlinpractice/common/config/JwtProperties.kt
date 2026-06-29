package com.example.springbootkotlinpractice.common.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "jwt")
data class JwtProperties(
    val secret: String,
    val accessTokenValidityMs: Long,
    val refreshTokenValidityMs: Long,
)
