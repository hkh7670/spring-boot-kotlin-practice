package com.example.springbootkotlinpractice.domain.auth.service

import com.example.springbootkotlinpractice.common.redis.RedisRepository
import com.example.springbootkotlinpractice.common.security.TotpProvider
import com.example.springbootkotlinpractice.domain.auth.dto.TotpEnrollResponse
import com.example.springbootkotlinpractice.domain.member.entity.Member
import com.example.springbootkotlinpractice.domain.member.repository.MemberRepository
import com.example.springbootkotlinpractice.enums.ResponseCodeEnum
import com.example.springbootkotlinpractice.exception.ApiErrorException
import java.time.Duration
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

// TOTP 시크릿의 등록/해제 생명주기만 담당한다 (로그인 시 토큰 발급은 AuthService.totpLogin() 담당).
// 등록 확정 전 시크릿은 Redis에만 짧은 TTL로 보관하고, 첫 코드 검증에 성공했을 때만 Member에
// 영구 저장한다 — 등록 도중 이탈/실패로 계정이 잠기는 것을 방지하기 위함.
@Service
class TotpService(
    private val memberRepository: MemberRepository,
    private val totpProvider: TotpProvider,
    private val redisRepository: RedisRepository,
) {
    companion object {
        private const val ENROLL_KEY_PREFIX = "totp-enroll:"
        private val ENROLL_TTL = Duration.ofMinutes(5)
    }

    @Transactional(readOnly = true)
    fun enroll(memberId: Long): TotpEnrollResponse {
        val member = memberRepository.findByIdOrNull(memberId)
            ?: throw ApiErrorException(ResponseCodeEnum.NOT_FOUND_USER)
        if (member.totpEnabled) {
            throw ApiErrorException(ResponseCodeEnum.TOTP_ALREADY_ENABLED)
        }

        val secret = totpProvider.generateSecret()
        redisRepository.save(ENROLL_KEY_PREFIX + memberId, secret, ENROLL_TTL)
        return TotpEnrollResponse(
            secret = secret,
            otpAuthUri = totpProvider.buildOtpAuthUri(requireNotNull(member.email) { "EMAIL 회원은 email이 필수" }, secret),
        )
    }

    @Transactional
    fun confirmEnroll(memberId: Long, code: String) {
        val member = memberRepository.findByIdOrNull(memberId)
            ?: throw ApiErrorException(ResponseCodeEnum.NOT_FOUND_USER)
        val pendingSecret = redisRepository.find(ENROLL_KEY_PREFIX + memberId)
            ?: throw ApiErrorException(ResponseCodeEnum.NOT_FOUND_TOTP_ENROLLMENT)
        if (!totpProvider.verifyCode(pendingSecret, code)) {
            throw ApiErrorException(ResponseCodeEnum.INVALID_TOTP_CODE)
        }

        member.enableTotp(pendingSecret)
        redisRepository.delete(ENROLL_KEY_PREFIX + memberId)
    }

    @Transactional
    fun disable(memberId: Long, code: String) {
        val member = memberRepository.findByIdOrNull(memberId)
            ?: throw ApiErrorException(ResponseCodeEnum.NOT_FOUND_USER)
        if (!member.totpEnabled) {
            throw ApiErrorException(ResponseCodeEnum.TOTP_NOT_ENABLED)
        }
        val secret = requireNotNull(member.totpSecret) { "totpEnabled=true인데 secret이 없음" }
        if (!totpProvider.verifyCode(secret, code)) {
            throw ApiErrorException(ResponseCodeEnum.INVALID_TOTP_CODE)
        }

        member.disableTotp()
    }

    // AuthService.totpLogin()에서 사용 — 로그인 완료 흐름 자체는 AuthService가 소유하므로 여기선 코드 검증만 담당
    fun verifyLoginCode(member: Member, code: String): Boolean {
        return member.totpSecret?.let { totpProvider.verifyCode(it, code) } ?: false
    }
}
