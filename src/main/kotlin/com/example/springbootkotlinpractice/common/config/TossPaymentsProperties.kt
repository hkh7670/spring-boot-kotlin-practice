package com.example.springbootkotlinpractice.common.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "toss.payments")
data class TossPaymentsProperties(
    val secretKey: String,
)
