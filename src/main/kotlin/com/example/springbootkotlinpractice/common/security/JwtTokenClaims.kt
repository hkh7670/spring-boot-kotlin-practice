package com.example.springbootkotlinpractice.common.security

import com.example.springbootkotlinpractice.enums.JoinProvider
import com.example.springbootkotlinpractice.enums.Role
import com.example.springbootkotlinpractice.enums.TokenType

data class JwtTokenClaims(
    private val id: Long,
    private val tokenType: TokenType,
    private val provider: JoinProvider? = null,
    private val role: Role? = null,
) {
    companion object {
        fun of(id: Long, tokenType: TokenType, provider: JoinProvider, role: Role): JwtTokenClaims {
            return JwtTokenClaims(
                id = id,
                tokenType = tokenType,
                provider = provider,
                role = role,
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
            provider?.let { put("provider", it) }
            role?.let { put("role", it) }
        }
    }
}
