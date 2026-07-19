package com.example.springbootkotlinpractice.common.oauth

import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap

// OAuth Provider(Google/Kakao/Naver) 공통 토큰 교환 요청. 토큰 엔드포인트는 application/x-www-form-urlencoded 를 요구하므로
// 전송 직전 toFormData() 로 MultiValueMap 변환 후 사용한다 (FormHttpMessageConverter 가 이 타입만 form-urlencoded 로 직렬화함)
data class TokenRequest(
    val code: String,
    val redirectUri: String,
    val clientId: String,
    val clientSecret: String? = null,
    val codeVerifier: String? = null,
    val grantType: String = "authorization_code",
) {
    fun toFormData(): MultiValueMap<String, String> {
        return LinkedMultiValueMap<String, String>().apply {
            add("grant_type", grantType)
            add("code", code)
            add("redirect_uri", redirectUri)
            add("client_id", clientId)
            clientSecret?.let { add("client_secret", it) }
            codeVerifier?.let { add("code_verifier", it) }
        }
    }
}
