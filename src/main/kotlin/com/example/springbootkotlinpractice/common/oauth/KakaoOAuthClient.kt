package com.example.springbootkotlinpractice.common.oauth

import com.example.springbootkotlinpractice.enums.JoinProvider
import com.example.springbootkotlinpractice.enums.ResponseCodeEnum
import com.example.springbootkotlinpractice.exception.ApiErrorException
import org.springframework.stereotype.Component

@Component
class KakaoOAuthClient(
    private val kakaoOAuthApi: KakaoOAuthApi,
) : OAuthClient {

    override val provider: JoinProvider = JoinProvider.KAKAO

    override fun getUserInfo(accessToken: String): OAuthUserInfo {
        val response = fetchUserInfo(accessToken)
        val account = response.kakaoAccount
        return OAuthUserInfo(
            providerId = response.id.toString(),
            email = account.email,
            nickname = account.profile.nickname,
        )
    }

    private fun fetchUserInfo(accessToken: String): KakaoUserInfoResponse {
        return runCatching { kakaoOAuthApi.getUserInfo("Bearer $accessToken") }
            .getOrElse {
                if (it is ApiErrorException) {
                    throw it
                }
                throw ApiErrorException(ResponseCodeEnum.EXTERNAL_SERVER_ERROR)
            }
    }
}
