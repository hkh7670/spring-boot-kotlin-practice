package com.example.springbootkotlinpractice.common.oauth.kakao

import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.service.annotation.PostExchange

interface KakaoTokenApi {

    @PostExchange("/oauth/token")
    fun exchangeToken(@RequestBody body: MultiValueMap<String, String>): KakaoTokenResponse
}

data class KakaoTokenResponse(
    @JsonProperty("token_type")
    val tokenType: String,
    @JsonProperty("access_token")
    val accessToken: String,
    @JsonProperty("id_token")
    val idToken: String?,
    @JsonProperty("expires_in")
    val expiresIn: Long,
    @JsonProperty("refresh_token")
    val refreshToken: String?,
    @JsonProperty("refresh_token_expires_in")
    val refreshTokenExpiresIn: Long?,
    val scope: String?,
)
