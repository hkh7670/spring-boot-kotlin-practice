package com.example.springbootkotlinpractice.common.security

import com.example.springbootkotlinpractice.enums.JoinProvider
import com.example.springbootkotlinpractice.enums.TokenType

data class JwtTokenClaims(
    private val id: Long,
    private val tokenType: TokenType,
    private val provider: JoinProvider? = null,
) {
    companion object {
        fun of(id: Long, tokenType: TokenType, provider: JoinProvider): JwtTokenClaims {
            return JwtTokenClaims(
                id = id,
                tokenType = tokenType,
                provider = provider,
            )
        }

        fun of(id: Long, tokenType: TokenType): JwtTokenClaims {
            return JwtTokenClaims(
                id = id,
                tokenType = tokenType,
            )
        }
    }

    fun toMap(): Map<String, Any> {
        return buildMap {
            put("id", id)
            put("tokenType", tokenType)
            if (provider != null) {
                put("provider", provider)
            }
        }
    }
}
