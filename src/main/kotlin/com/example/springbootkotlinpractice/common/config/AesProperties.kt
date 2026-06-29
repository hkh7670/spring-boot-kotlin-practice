package com.example.springbootkotlinpractice.common.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "aes")
data class AesProperties(
    val secret: String,
    val initVector: String,
)
