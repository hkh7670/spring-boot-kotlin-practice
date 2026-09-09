package com.example.springbootkotlinpractice.common.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "opensearch")
data class OpenSearchProperties(
    val host: String,
    val port: Int,
    val scheme: String,
)
