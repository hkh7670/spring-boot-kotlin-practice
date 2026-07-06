package com.example.springbootkotlinpractice.common.oauth

import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.service.annotation.PostExchange

interface GoogleTokenApi {

    @PostExchange("/token")
    fun exchangeToken(@RequestBody body: MultiValueMap<String, String>): GoogleTokenResponse
}

data class GoogleTokenResponse(
    @JsonProperty("access_token")
    val accessToken: String,
    @JsonProperty("expires_in")
    val expiresIn: Long,
    @JsonProperty("token_type")
    val tokenType: String,
    @JsonProperty("id_token")
    val idToken: String?,
    @JsonProperty("refresh_token")
    val refreshToken: String?,
    val scope: String?,
)
