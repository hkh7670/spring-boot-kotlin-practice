package com.example.springbootkotlinpractice.common.oauth

import com.fasterxml.jackson.annotation.JsonProperty
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
    val email: String?,
    val nickname: String?,
    val name: String,
    @JsonProperty("profile_image")
    val profileImage: String?,
    val age: String?,
    val gender: String?,
    val birthday: String?,
    val birthyear: String?,
    val mobile: String?,
)
