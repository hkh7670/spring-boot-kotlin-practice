package com.example.springbootkotlinpractice.domain.auth.service

import com.example.springbootkotlinpractice.common.redis.RedisRepository
import com.example.springbootkotlinpractice.domain.auth.dto.OAuthLoginResponse
import com.example.springbootkotlinpractice.enums.ResponseCodeEnum
import com.example.springbootkotlinpractice.exception.ApiErrorException
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.UUID

// oauth2Login 콜백 처리 후 프론트로 결과를 넘길 때, 토큰을 리다이렉트 URL에 직접 싣지 않기 위한 1회용 relay code
@Service
class OAuthRelayCodeService(
    private val redisRepository: RedisRepository,
    private val objectMapper: ObjectMapper,
) {
    companion object {
        private const val RELAY_CODE_KEY_PREFIX = "oauth-relay-code:"
        private val RELAY_CODE_TTL: Duration = Duration.ofSeconds(60)
    }

    fun issue(response: OAuthLoginResponse): String {
        val code = UUID.randomUUID().toString()
        redisRepository.save(
            RELAY_CODE_KEY_PREFIX + code,
            objectMapper.writeValueAsString(response),
            RELAY_CODE_TTL,
        )
        return code
    }

    // 1회용이므로 조회 즉시 삭제한다
    fun consume(code: String): OAuthLoginResponse {
        val key = RELAY_CODE_KEY_PREFIX + code
        val json = redisRepository.find(key)
            ?: throw ApiErrorException(ResponseCodeEnum.BAD_REQUEST)
        redisRepository.delete(key)
        return objectMapper.readValue(json, OAuthLoginResponse::class.java)
    }
}
