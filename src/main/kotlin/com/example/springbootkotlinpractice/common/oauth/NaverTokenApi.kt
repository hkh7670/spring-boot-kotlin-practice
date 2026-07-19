package com.example.springbootkotlinpractice.common.oauth

import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.service.annotation.PostExchange

interface NaverTokenApi {

    @PostExchange("/oauth2.0/token")
    fun exchangeToken(@RequestBody body: MultiValueMap<String, String>): NaverTokenResponse
}

data class NaverTokenResponse(
    @JsonProperty("access_token")
    val accessToken: String,
    @JsonProperty("refresh_token")
    val refreshToken: String?,
    @JsonProperty("token_type")
    val tokenType: String,
    @JsonProperty("expires_in")
    val expiresIn: String,
)
