package com.example.springbootkotlinpractice.common.security

import com.example.springbootkotlinpractice.common.config.JwtProperties
import com.example.springbootkotlinpractice.enums.JoinProvider
import com.example.springbootkotlinpractice.enums.Role
import com.example.springbootkotlinpractice.enums.TokenType
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    private val jwtProperties: JwtProperties,
) {
    private val key: SecretKey = Keys.hmacShaKeyFor(
        jwtProperties.secret.toByteArray(StandardCharsets.UTF_8)
    )

    fun createAccessToken(memberId: Long, joinProvider: JoinProvider, role: Role): String {
        return buildToken(
            memberId = memberId,
            validityMs = jwtProperties.accessTokenValidityMs,
            extraClaims = JwtTokenClaims.of(
                id = memberId,
                provider = joinProvider,
                tokenType = TokenType.ACCESS_TOKEN,
                role = role,
            ).toMap()
        )
    }

    fun createRefreshToken(memberId: Long): String {
        return buildToken(
            memberId = memberId,
            validityMs = jwtProperties.refreshTokenValidityMs,
            extraClaims = JwtTokenClaims.of(
                id = memberId,
                tokenType = TokenType.REFRESH_TOKEN
            ).toMap()
        )
    }

    fun getMemberId(token: String): Long {
        return parse(token).subject.toLong()
    }

    fun getRole(token: String): Role {
        return Role.valueOf(parse(token)["role"].toString())
    }

    fun isValid(token: String): Boolean {
        return runCatching { parse(token) }.isSuccess
    }

    private fun buildToken(memberId: Long, validityMs: Long, extraClaims: Map<String, Any>): String {
        val now = System.currentTimeMillis()
        return Jwts.builder()
            .subject(memberId.toString())
            .claims(extraClaims)
            .issuedAt(Date(now))
            .expiration(Date(now + validityMs))
            .signWith(key)
            .compact()
    }

    private fun parse(token: String) =
        Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
}
