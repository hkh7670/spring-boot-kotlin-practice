package com.example.springbootkotlinpractice.common.oauth

import org.springframework.http.HttpHeaders
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.service.annotation.GetExchange

interface NaverOAuthApi {

    @GetExchange("/v1/nid/me")
    fun getUserInfo(@RequestHeader(HttpHeaders.AUTHORIZATION) authorization: String): NaverUserInfoResponse
}

data class NaverUserInfoResponse(
    val response: NaverAccount,
)

data class NaverAccount(
    val id: String,
    val email: String,
    val name: String,
)
