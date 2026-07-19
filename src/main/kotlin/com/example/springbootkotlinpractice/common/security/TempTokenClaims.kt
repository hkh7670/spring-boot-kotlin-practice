package com.example.springbootkotlinpractice.common.security

import com.example.springbootkotlinpractice.enums.JoinProvider
import com.example.springbootkotlinpractice.enums.TokenType
import io.jsonwebtoken.Claims

data class TempTokenClaims(
    val providerId: String,
    val provider: JoinProvider,
    val email: String?,
    val nickname: String?,
) {
    companion object {
        fun of(
            providerId: String,
            provider: JoinProvider,
            email: String?,
            nickname: String?,
        ): TempTokenClaims {
            return TempTokenClaims(
                providerId = providerId,
                provider = provider,
                email = email,
                nickname = nickname,
            )
        }

        fun from(claims: Claims): TempTokenClaims {
            return TempTokenClaims(
                providerId = claims.subject,
                provider = JoinProvider.valueOf(claims["provider"].toString()),
                email = claims["email"]?.toString(),
                nickname = claims["nickname"]?.toString(),
            )
        }
    }

    fun toMap(): Map<String, Any> {
        return buildMap {
            put("tokenType", TokenType.TEMP_TOKEN.name)
            put("provider", provider.name)
            email?.let { put("email", it) }
            nickname?.let { put("nickname", it) }
        }
    }
}
