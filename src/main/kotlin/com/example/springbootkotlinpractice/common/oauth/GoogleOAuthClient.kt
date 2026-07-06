package com.example.springbootkotlinpractice.common.oauth

import com.example.springbootkotlinpractice.common.config.GoogleOAuthProperties
import com.example.springbootkotlinpractice.enums.JoinProvider
import com.example.springbootkotlinpractice.enums.ResponseCodeEnum
import com.example.springbootkotlinpractice.exception.ApiErrorException
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap

@Component
class GoogleOAuthClient(
    private val googleOAuthApi: GoogleOAuthApi,
    private val googleTokenApi: GoogleTokenApi,
    private val googleOAuthProperties: GoogleOAuthProperties,
) : OAuthClient {

    override val provider: JoinProvider = JoinProvider.GOOGLE

    override fun getUserInfo(accessToken: String): OAuthUserInfo {
        return toOAuthUserInfo(fetchUserInfo(accessToken))
    }

    // Authorization Code + PKCE 로 Access Token 교환 후 사용자 정보 조회
    // code_verifier 없이는 code 만으로 토큰 교환이 불가능해 탈취된 code 단독으로는 악용할 수 없다
    fun getUserInfoByAuthorizationCode(code: String, codeVerifier: String, redirectUri: String): OAuthUserInfo {
        val tokenResponse = exchangeToken(code, codeVerifier, redirectUri)
        return toOAuthUserInfo(fetchUserInfo(tokenResponse.accessToken))
    }

    private fun exchangeToken(code: String, codeVerifier: String, redirectUri: String): GoogleTokenResponse {
        return runCatching { googleTokenApi.exchangeToken(buildTokenRequestBody(code, codeVerifier, redirectUri)) }
            .getOrElse {
                if (it is ApiErrorException) {
                    throw it
                }
                throw ApiErrorException(ResponseCodeEnum.EXTERNAL_SERVER_ERROR)
            }
    }

    private fun buildTokenRequestBody(
        code: String,
        codeVerifier: String,
        redirectUri: String,
    ): MultiValueMap<String, String> {
        return LinkedMultiValueMap<String, String>().apply {
            add("grant_type", "authorization_code")
            add("code", code)
            add("code_verifier", codeVerifier)
            add("redirect_uri", redirectUri)
            add("client_id", googleOAuthProperties.clientId)
            add("client_secret", googleOAuthProperties.clientSecret)
        }
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
