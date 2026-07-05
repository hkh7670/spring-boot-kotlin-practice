package com.example.springbootkotlinpractice.common.oauth

import com.example.springbootkotlinpractice.enums.JoinProvider
import com.example.springbootkotlinpractice.enums.ResponseCodeEnum
import com.example.springbootkotlinpractice.exception.ApiErrorException
import org.springframework.stereotype.Component

@Component
class NaverOAuthClient(
    private val naverOAuthApi: NaverOAuthApi,
) : OAuthClient {

    override val provider: JoinProvider = JoinProvider.NAVER

    override fun getUserInfo(accessToken: String): OAuthUserInfo {
        val response = fetchUserInfo(accessToken)
        val account = response.response
        return OAuthUserInfo(
            providerId = account.id,
            email = account.email,
            nickname = account.name,
        )
    }

    private fun fetchUserInfo(accessToken: String): NaverUserInfoResponse {
        return runCatching { naverOAuthApi.getUserInfo("Bearer $accessToken") }
            .getOrElse {
                if (it is ApiErrorException) {
                    throw it
                }
                throw ApiErrorException(ResponseCodeEnum.EXTERNAL_SERVER_ERROR)
            }
    }
}
