package com.example.springbootkotlinpractice.common.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "oauth.kakao")
data class KakaoOAuthProperties(
    val clientId: String,
    // 카카오 개발자 콘솔에서 "Client Secret" 보안 기능을 활성화한 경우에만 필요
    val clientSecret: String?,
)
