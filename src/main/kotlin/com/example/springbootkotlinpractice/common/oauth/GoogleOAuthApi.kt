package com.example.springbootkotlinpractice.common.oauth

import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.http.HttpHeaders
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.service.annotation.GetExchange

interface GoogleOAuthApi {

    @GetExchange("/oauth2/v3/userinfo")
    fun getUserInfo(@RequestHeader(HttpHeaders.AUTHORIZATION) authorization: String): GoogleUserInfoResponse
}

data class GoogleUserInfoResponse(
    val sub: String,
    val name: String,
    @JsonProperty("given_name")
    val givenName: String,
    @JsonProperty("family_name")
    val familyName: String,
    val picture: String?,
    val email: String?,
    @JsonProperty("email_verified")
    val emailVerified: Boolean,
    val locale: String?,
    val hd: String?,
)
