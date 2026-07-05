package com.example.springbootkotlinpractice.common.oauth

import org.springframework.http.HttpHeaders
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.service.annotation.GetExchange

interface GoogleOAuthApi {

    @GetExchange("/oauth2/v3/userinfo")
    fun getUserInfo(@RequestHeader(HttpHeaders.AUTHORIZATION) authorization: String): GoogleUserInfoResponse
}

data class GoogleUserInfoResponse(
    val sub: String,
    val email: String,
    val name: String,
)
