package com.example.springbootkotlinpractice.common.oauth.naver

import com.example.springbootkotlinpractice.common.config.NaverOAuthProperties
import com.example.springbootkotlinpractice.common.oauth.OAuthClient
import com.example.springbootkotlinpractice.common.oauth.OAuthUserInfo
import com.example.springbootkotlinpractice.common.oauth.TokenRequest
import com.example.springbootkotlinpractice.enums.JoinProvider
import com.example.springbootkotlinpractice.enums.ResponseCodeEnum
import com.example.springbootkotlinpractice.exception.ApiErrorException
import org.springframework.stereotype.Component

@Component
class NaverOAuthClient(
    private val naverOAuthApi: NaverOAuthApi,
    private val naverTokenApi: NaverTokenApi,
    private val naverOAuthProperties: NaverOAuthProperties,
) : OAuthClient {

    override val provider: JoinProvider = JoinProvider.NAVER

    // 네이버 토큰 API는 PKCE(code_verifier)를 지원하지 않아 codeVerifier는 사용하지 않고 client_secret으로만 code 탈취를 방어한다
    override fun getUserInfoByAuthorizationCode(
        code: String,
        codeVerifier: String,
        redirectUri: String
    ): OAuthUserInfo {
        val tokenResponse = exchangeToken(code, redirectUri)
        val response = fetchUserInfo(tokenResponse.accessToken)
        val account = response.response
        return OAuthUserInfo(
            providerId = account.id,
            email = account.email,
            nickname = account.name,
        )
    }

    private fun exchangeToken(code: String, redirectUri: String): NaverTokenResponse {
        return runCatching { naverTokenApi.exchangeToken(buildTokenRequest(code, redirectUri).toFormData()) }
            .getOrElse {
                if (it is ApiErrorException) {
                    throw it
                }
                throw ApiErrorException(ResponseCodeEnum.EXTERNAL_SERVER_ERROR)
            }
    }

    private fun buildTokenRequest(
        code: String,
        redirectUri: String,
    ): TokenRequest {
        return TokenRequest(
            code = code,
            redirectUri = redirectUri,
            clientId = naverOAuthProperties.clientId,
            clientSecret = naverOAuthProperties.clientSecret,
        )
    }

    private fun fetchUserInfo(accessToken: String): NaverUserInfoResponse {
        return runCatching {
            naverOAuthApi.getUserInfo("Bearer $accessToken")
        }.getOrElse {
            if (it is ApiErrorException) {
                throw it
            }
            throw ApiErrorException(ResponseCodeEnum.EXTERNAL_SERVER_ERROR)
        }
    }
}
