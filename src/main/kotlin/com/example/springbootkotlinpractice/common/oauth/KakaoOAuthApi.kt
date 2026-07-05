package com.example.springbootkotlinpractice.common.oauth

import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.http.HttpHeaders
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.service.annotation.GetExchange

interface KakaoOAuthApi {

    @GetExchange("/v2/user/me")
    fun getUserInfo(@RequestHeader(HttpHeaders.AUTHORIZATION) authorization: String): KakaoUserInfoResponse
}

data class KakaoUserInfoResponse(
    val id: Long,
    @JsonProperty("kakao_account")
    val kakaoAccount: KakaoAccount,
)

data class KakaoAccount(
    val email: String,
    val profile: KakaoProfile,
)

data class KakaoProfile(
    val nickname: String,
)
