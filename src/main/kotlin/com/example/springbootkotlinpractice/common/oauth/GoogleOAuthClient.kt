package com.example.springbootkotlinpractice.common.oauth

import com.example.springbootkotlinpractice.enums.JoinProvider
import com.example.springbootkotlinpractice.enums.ResponseCodeEnum
import com.example.springbootkotlinpractice.exception.ApiErrorException
import org.springframework.stereotype.Component

@Component
class GoogleOAuthClient(
    private val googleOAuthApi: GoogleOAuthApi,
) : OAuthClient {

    override val provider: JoinProvider = JoinProvider.GOOGLE

    override fun getUserInfo(accessToken: String): OAuthUserInfo {
        val response = fetchUserInfo(accessToken)
        return OAuthUserInfo(
            providerId = response.sub,
            email = response.email,
            nickname = response.name,
        )
    }

    private fun fetchUserInfo(accessToken: String): GoogleUserInfoResponse {
        return runCatching { googleOAuthApi.getUserInfo("Bearer $accessToken") }
            .getOrElse {
                if (it is ApiErrorException) {
                    throw it
                }
                throw ApiErrorException(ResponseCodeEnum.EXTERNAL_SERVER_ERROR)
            }
    }
}
