package com.example.springbootkotlinpractice.common.oauth.kakao

import com.example.springbootkotlinpractice.common.config.KakaoOAuthProperties
import com.example.springbootkotlinpractice.common.oauth.OAuthClient
import com.example.springbootkotlinpractice.common.oauth.OAuthUserInfo
import com.example.springbootkotlinpractice.common.oauth.TokenRequest
import com.example.springbootkotlinpractice.enums.JoinProvider
import com.example.springbootkotlinpractice.enums.ResponseCodeEnum
import com.example.springbootkotlinpractice.exception.ApiErrorException
import org.springframework.stereotype.Component

@Component
class KakaoOAuthClient(
    private val kakaoOAuthApi: KakaoOAuthApi,
    private val kakaoTokenApi: KakaoTokenApi,
    private val kakaoOAuthProperties: KakaoOAuthProperties,
) : OAuthClient {

    override val provider: JoinProvider = JoinProvider.KAKAO

    // 카카오 토큰 API는 PKCE(code_verifier)를 지원하지 않아 codeVerifier는 사용하지 않고 client_secret으로만 code 탈취를 방어한다
    override fun getUserInfoByAuthorizationCode(code: String, codeVerifier: String, redirectUri: String): OAuthUserInfo {
        val tokenResponse = exchangeToken(code, redirectUri)
        val response = fetchUserInfo(tokenResponse.accessToken)
        val account = response.kakaoAccount
        return OAuthUserInfo(
            providerId = response.id.toString(),
            email = account.email,
            nickname = account.profile.nickname,
        )
    }

    private fun exchangeToken(code: String, redirectUri: String): KakaoTokenResponse {
        return runCatching { kakaoTokenApi.exchangeToken(buildTokenRequest(code, redirectUri).toFormData()) }
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
            clientId = kakaoOAuthProperties.clientId,
            clientSecret = kakaoOAuthProperties.clientSecret,
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
