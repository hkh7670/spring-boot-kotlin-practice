package com.example.springbootkotlinpractice.common.oauth

import com.example.springbootkotlinpractice.common.config.GoogleOAuthProperties
import com.example.springbootkotlinpractice.enums.JoinProvider
import com.example.springbootkotlinpractice.enums.ResponseCodeEnum
import com.example.springbootkotlinpractice.exception.ApiErrorException
import org.springframework.stereotype.Component

@Component
class GoogleOAuthClient(
    private val googleOAuthApi: GoogleOAuthApi,
    private val googleTokenApi: GoogleTokenApi,
    private val googleOAuthProperties: GoogleOAuthProperties,
) : OAuthClient {

    override val provider: JoinProvider = JoinProvider.GOOGLE

    override fun getUserInfoByAuthorizationCode(code: String, codeVerifier: String, redirectUri: String): OAuthUserInfo {
        val tokenResponse = exchangeToken(code, codeVerifier, redirectUri)
        return toOAuthUserInfo(fetchUserInfo(tokenResponse.accessToken))
    }

    private fun exchangeToken(code: String, codeVerifier: String, redirectUri: String): GoogleTokenResponse {
        return runCatching { googleTokenApi.exchangeToken(buildTokenRequest(code, codeVerifier, redirectUri).toFormData()) }
            .getOrElse {
                if (it is ApiErrorException) {
                    throw it
                }
                throw ApiErrorException(ResponseCodeEnum.EXTERNAL_SERVER_ERROR)
            }
    }

    private fun buildTokenRequest(
        code: String,
        codeVerifier: String,
        redirectUri: String,
    ): TokenRequest {
        return TokenRequest(
            code = code,
            redirectUri = redirectUri,
            clientId = googleOAuthProperties.clientId,
            clientSecret = googleOAuthProperties.clientSecret,
            codeVerifier = codeVerifier,
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

    private fun toOAuthUserInfo(response: GoogleUserInfoResponse): OAuthUserInfo {
        return OAuthUserInfo(
            providerId = response.sub,
            email = response.email,
            nickname = response.name,
        )
    }
}
