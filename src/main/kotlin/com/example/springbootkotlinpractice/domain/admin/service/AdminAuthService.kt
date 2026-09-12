package com.example.springbootkotlinpractice.domain.admin.service

import com.example.springbootkotlinpractice.common.config.JwtProperties
import com.example.springbootkotlinpractice.common.redis.RedisRepository
import com.example.springbootkotlinpractice.common.security.JwtTokenProvider
import com.example.springbootkotlinpractice.domain.admin.dto.AdminLoginRequest
import com.example.springbootkotlinpractice.domain.admin.repository.AdminRepository
import com.example.springbootkotlinpractice.domain.auth.dto.AuthTokenResponse
import com.example.springbootkotlinpractice.enums.AdminStatus
import com.example.springbootkotlinpractice.enums.ResponseCodeEnum
import com.example.springbootkotlinpractice.exception.ApiErrorException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration

@Service
class AdminAuthService(
    private val adminRepository: AdminRepository,
    private val jwtTokenProvider: JwtTokenProvider,
    private val jwtProperties: JwtProperties,
    private val passwordEncoder: PasswordEncoder,
    private val redisRepository: RedisRepository,
) {

    @Transactional(readOnly = true)
    fun login(request: AdminLoginRequest): AuthTokenResponse {
        val admin = adminRepository.findByEmail(request.email)
            ?: throw ApiErrorException(ResponseCodeEnum.INVALID_CREDENTIALS)

        if (!passwordEncoder.matches(request.password, admin.password)) {
            throw ApiErrorException(ResponseCodeEnum.INVALID_CREDENTIALS)
        }
        if (admin.status != AdminStatus.ACTIVE) {
            throw ApiErrorException(ResponseCodeEnum.ADMIN_LOGIN_NOT_ALLOWED)
        }

        val refreshToken = jwtTokenProvider.createRefreshToken(admin.id)
        redisRepository.save(
            ADMIN_REFRESH_TOKEN_KEY_PREFIX + admin.id,
            refreshToken,
            Duration.ofMillis(jwtProperties.refreshTokenValidityMs),
        )
        return AuthTokenResponse(
            accessToken = jwtTokenProvider.createAdminAccessToken(admin.id),
            refreshToken = refreshToken,
        )
    }

    companion object {
        private const val ADMIN_REFRESH_TOKEN_KEY_PREFIX = "admin-refresh-token:"
    }
}
