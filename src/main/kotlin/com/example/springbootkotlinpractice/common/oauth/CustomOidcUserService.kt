package com.example.springbootkotlinpractice.common.oauth

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.stereotype.Service

// Google(OIDC) 유저정보를 OAuthUserInfo로 매핑한다
@Service
class CustomOidcUserService : OidcUserService() {

    override fun loadUser(userRequest: OidcUserRequest): OidcUser {
        val oidcUser = super.loadUser(userRequest)
        val oAuthUserInfo = OAuthUserInfo(
            providerId = oidcUser.subject,
            email = oidcUser.email,
            nickname = oidcUser.fullName ?: oidcUser.givenName,
        )
        return CustomOidcUser(oAuthUserInfo, oidcUser.idToken, oidcUser.userInfo)
    }
}
