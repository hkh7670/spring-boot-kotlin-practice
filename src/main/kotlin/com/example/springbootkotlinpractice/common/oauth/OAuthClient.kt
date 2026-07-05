package com.example.springbootkotlinpractice.common.oauth

import com.example.springbootkotlinpractice.enums.JoinProvider

interface OAuthClient {

    val provider: JoinProvider

    // 클라이언트가 발급받은 accessToken 을 provider 에 검증하여 사용자 정보를 조회한다
    fun getUserInfo(accessToken: String): OAuthUserInfo
}
