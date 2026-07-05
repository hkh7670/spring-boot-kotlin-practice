package com.example.springbootkotlinpractice.common.oauth

import com.example.springbootkotlinpractice.enums.JoinProvider
import com.example.springbootkotlinpractice.enums.ResponseCodeEnum
import com.example.springbootkotlinpractice.exception.ApiErrorException
import org.springframework.stereotype.Component

@Component
class OAuthClientResolver(
    oAuthClients: List<OAuthClient>,
) {
    private val clientsByProvider: Map<JoinProvider, OAuthClient> = oAuthClients.associateBy { it.provider }

    fun resolve(provider: JoinProvider): OAuthClient {
        return clientsByProvider[provider]
            ?: throw ApiErrorException(ResponseCodeEnum.BAD_REQUEST)
    }
}
