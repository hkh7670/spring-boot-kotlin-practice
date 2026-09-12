package com.example.springbootkotlinpractice.common.security

import com.example.springbootkotlinpractice.enums.JoinProvider
import com.example.springbootkotlinpractice.enums.Role
import com.example.springbootkotlinpractice.enums.TokenType

data class JwtTokenClaims(
    private val memberId: Long,
    private val tokenType: TokenType,
    private val email: String? = null,
    private val provider: JoinProvider? = null,
    private val role: String? = null,
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
                role = role.name,
            )
        }

        fun of(id: Long, tokenType: TokenType): JwtTokenClaims {
            return JwtTokenClaims(
                memberId = id,
                tokenType = tokenType,
            )
        }

        // Admin은 Role enum(Member 전용)과 무관하게 리터럴 권한 문자열을 그대로 클레임에 싣는다.
        fun of(id: Long, tokenType: TokenType, role: String): JwtTokenClaims {
            return JwtTokenClaims(
                memberId = id,
                tokenType = tokenType,
                role = role,
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
