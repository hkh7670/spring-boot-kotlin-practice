package com.example.springbootkotlinpractice.common.security

import com.example.springbootkotlinpractice.enums.JoinProvider
import com.example.springbootkotlinpractice.enums.Role
import com.example.springbootkotlinpractice.enums.TokenType

data class JwtTokenClaims(
    private val memberId: Long,
    private val tokenType: TokenType,
    private val email: String? = null,
    private val provider: JoinProvider? = null,
    private val role: Role? = null,
) {
    companion object {
        fun of(
            memberId: Long,
            email: String?,
            tokenType: TokenType,
            provider: JoinProvider,
            role: Role
        ): JwtTokenClaims {
            return JwtTokenClaims(
                memberId = memberId,
                email = email,
                tokenType = tokenType,
                provider = provider,
                role = role,
            )
        }

        fun of(id: Long, tokenType: TokenType): JwtTokenClaims {
            return JwtTokenClaims(
                memberId = id,
                tokenType = tokenType,
            )
        }
    }

    fun toMap(): Map<String, Any> {
        return buildMap {
            put("id", memberId)
            put("tokenType", tokenType)
            email?.let { put("email", it) }
            provider?.let { put("provider", it) }
            role?.let { put("role", it) }
        }
    }
}
