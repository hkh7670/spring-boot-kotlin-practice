package com.example.springbootkotlinpractice.common.oauth

import com.example.springbootkotlinpractice.enums.ResponseCodeEnum
import com.example.springbootkotlinpractice.exception.ApiErrorException
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service

// Kakao/Naver 처럼 Spring Security에 내장 provider가 없는 곳들의 attribute를 OAuthUserInfo로 매핑한다
@Service
class CustomOAuth2UserService : DefaultOAuth2UserService() {

    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val oAuth2User = super.loadUser(userRequest)
        val registrationId = userRequest.clientRegistration.registrationId
        val nameAttributeKey = userRequest.clientRegistration.providerDetails
            .userInfoEndpoint.userNameAttributeName
            ?: throw ApiErrorException(ResponseCodeEnum.EXTERNAL_SERVER_ERROR)

        val oAuthUserInfo = when (registrationId) {
            "kakao" -> toKakaoUserInfo(oAuth2User.attributes)
            "naver" -> toNaverUserInfo(oAuth2User.attributes)
            else -> throw ApiErrorException(ResponseCodeEnum.EXTERNAL_SERVER_ERROR)
        }

        return CustomOAuth2User(oAuthUserInfo, oAuth2User.attributes, nameAttributeKey)
    }

    @Suppress("UNCHECKED_CAST")
    private fun toKakaoUserInfo(attributes: Map<String, Any>): OAuthUserInfo {
        val account = attributes["kakao_account"] as? Map<String, Any>
        val profile = account?.get("profile") as? Map<String, Any>
        return OAuthUserInfo(
            providerId = attributes["id"].toString(),
            email = account?.get("email") as? String,
            nickname = profile?.get("nickname") as? String,
        )
    }

    // 네이버 유저정보 응답은 실제 필드가 response 객체 하나에 중첩되어 있다
    @Suppress("UNCHECKED_CAST")
    private fun toNaverUserInfo(attributes: Map<String, Any>): OAuthUserInfo {
        val account = attributes["response"] as? Map<String, Any>
            ?: throw ApiErrorException(ResponseCodeEnum.EXTERNAL_SERVER_ERROR)
        return OAuthUserInfo(
            providerId = account["id"] as String,
            email = account["email"] as? String,
            nickname = account["name"] as? String,
        )
    }
}
