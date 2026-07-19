package com.example.springbootkotlinpractice.common.oauth

import com.example.springbootkotlinpractice.enums.JoinProvider

interface OAuthClient {

    val provider: JoinProvider

    // Authorization Code + PKCE 로 Access Token 교환 후 사용자 정보 조회
    // code_verifier 없이는 code 만으로 토큰 교환이 불가능해 탈취된 code 단독으로는 악용할 수 없다
    fun getUserInfoByAuthorizationCode(code: String, codeVerifier: String, redirectUri: String): OAuthUserInfo
}
