package com.example.springbootkotlinpractice.common.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "oauth.naver")
data class NaverOAuthProperties(
    val clientId: String,
    val clientSecret: String,
)
