package com.example.springbootkotlinpractice.domain.auth.service

import com.example.springbootkotlinpractice.domain.auth.dto.AuthTokenResponse
import com.example.springbootkotlinpractice.domain.auth.dto.EmailLoginRequest
import com.example.springbootkotlinpractice.domain.auth.dto.EmailLoginResponse
import com.example.springbootkotlinpractice.domain.auth.dto.EmailSignUpRequest
import com.example.springbootkotlinpractice.domain.auth.dto.OAuthLoginResponse
import com.example.springbootkotlinpractice.domain.auth.dto.OAuthSignUpRequest
import com.example.springbootkotlinpractice.domain.auth.dto.TotpLoginRequest
import com.example.springbootkotlinpractice.domain.auth.enums.EmailLoginStatus
import com.example.springbootkotlinpractice.domain.auth.enums.OAuthLoginStatus
import com.example.springbootkotlinpractice.common.config.JwtProperties
import com.example.springbootkotlinpractice.common.oauth.OAuthUserInfo
import com.example.springbootkotlinpractice.common.redis.RedisRepository
import com.example.springbootkotlinpractice.common.security.JwtTokenProvider
import com.example.springbootkotlinpractice.enums.JoinProvider
import com.example.springbootkotlinpractice.enums.ResponseCodeEnum
import com.example.springbootkotlinpractice.enums.Role
import com.example.springbootkotlinpractice.exception.ApiErrorException
import com.example.springbootkotlinpractice.domain.member.entity.Member
import com.example.springbootkotlinpractice.domain.member.repository.MemberRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration

@Service
class AuthService(
    private val memberRepository: MemberRepository,
    private val jwtTokenProvider: JwtTokenProvider,
    private val jwtProperties: JwtProperties,
    private val passwordEncoder: PasswordEncoder,
    private val redisRepository: RedisRepository,
    private val totpService: TotpService,
) {
    companion object {
        private const val REFRESH_TOKEN_KEY_PREFIX = "refresh-token:"
    }

    // 가입경로에 맞춰 회원 생성 후 토큰 발급
    @Transactional
    fun signUp(request: EmailSignUpRequest, joinProvider: JoinProvider): AuthTokenResponse {
        if (memberRepository.existsByEmailAndJoinProvider(request.email, joinProvider)) {
            throw ApiErrorException(ResponseCodeEnum.DUPLICATED_EMAIL)
        }
        val member = memberRepository.save(
            Member.of(
                lastName = request.lastName,
                firstName = request.firstName,
                birthDate = request.birthDate,
                phoneNumber = request.phoneNumber,
                email = request.email,
                password = passwordEncoder.encode(request.password)
                    ?: throw ApiErrorException(ResponseCodeEnum.INTERNAL_SERVER_ERROR),
                joinProvider = joinProvider,
            )
        )
        return issue(member.id, member.email, member.joinProvider)
    }

    // 이메일 + 비밀번호로 로그인 후, TOTP가 활성화된 회원이면 pending 토큰만 발급하고 최종 토큰은
    // totpLogin()에서 발급한다 (oauthLogin()의 LOGIN/NEED_SIGN_UP 상태분기와 동일한 모양)
    @Transactional(readOnly = true)
    fun login(request: EmailLoginRequest): EmailLoginResponse {
        val member = memberRepository.findByEmailAndJoinProvider(request.email, JoinProvider.EMAIL)
            ?: throw ApiErrorException(ResponseCodeEnum.INVALID_CREDENTIALS)

        if (member.password == null || !passwordEncoder.matches(
                request.password,
                member.password
            )
        ) {
            throw ApiErrorException(ResponseCodeEnum.INVALID_CREDENTIALS)
        }

        if (member.totpEnabled) {
            return EmailLoginResponse(
                status = EmailLoginStatus.NEED_TOTP,
                totpPendingToken = jwtTokenProvider.createTotpPendingToken(member.id),
            )
        }
        val tokens = issue(member.id, member.email, member.joinProvider)
        return EmailLoginResponse(
            status = EmailLoginStatus.LOGIN,
            accessToken = tokens.accessToken,
            refreshToken = tokens.refreshToken,
        )
    }

    // TOTP pending 토큰 + 코드로 2단계 인증 완료 후 최종 토큰 발급
    @Transactional(readOnly = true)
    fun totpLogin(request: TotpLoginRequest): AuthTokenResponse {
        val memberId = jwtTokenProvider.parseTotpPendingToken(request.totpPendingToken)
        val member = memberRepository.findByIdOrNull(memberId)
            ?: throw ApiErrorException(ResponseCodeEnum.NOT_FOUND_USER)
        if (!totpService.verifyLoginCode(member, request.code)) {
            throw ApiErrorException(ResponseCodeEnum.INVALID_TOTP_CODE)
        }
        return issue(member.id, member.email, member.joinProvider)
    }

    // oauth2Login 콜백 처리 후 기존 회원이면 LOGIN, 신규 회원이면 NEED_SIGN_UP 으로 분기
    @Transactional(readOnly = true)
    fun oauthLogin(
        provider: JoinProvider,
        userInfo: OAuthUserInfo
    ): OAuthLoginResponse {
        val member = memberRepository.findByProviderIdAndJoinProvider(userInfo.providerId, provider)

        return if (member != null) {
            val tokens = issue(member.id, member.email, member.joinProvider)
            OAuthLoginResponse(
                status = OAuthLoginStatus.LOGIN,
                accessToken = tokens.accessToken,
                refreshToken = tokens.refreshToken,
            )
        } else {
            OAuthLoginResponse(
                status = OAuthLoginStatus.NEED_SIGN_UP,
                tempToken = jwtTokenProvider.createTempToken(
                    providerId = userInfo.providerId,
                    provider = provider,
                    email = userInfo.email,
                    nickname = userInfo.nickname,
                ),
            )
        }
    }

    // tempToken + 추가 정보로 OAuth 회원가입 후 JWT 발급
    @Transactional
    fun oauthSignUp(request: OAuthSignUpRequest): AuthTokenResponse {
        val claims = jwtTokenProvider.parseTempToken(request.tempToken)

        if (memberRepository.existsByProviderIdAndJoinProvider(
                claims.providerId,
                claims.provider
            )
        ) {
            throw ApiErrorException(ResponseCodeEnum.ALREADY_REGISTERED_OAUTH)
        }

        val member = memberRepository.save(
            Member.ofOAuth(
                providerId = claims.providerId,
                email = claims.email,
                lastName = request.lastName,
                firstName = request.firstName,
                birthDate = request.birthDate,
                phoneNumber = request.phoneNumber,
                joinProvider = claims.provider,
            )
        )
        return issue(member.id, member.email, member.joinProvider)
    }

    // access + refresh 동시 발급. refreshToken은 Redis에 저장해 rotation의 기준값으로 사용한다
    fun issue(memberId: Long, email: String?, joinProvider: JoinProvider): AuthTokenResponse {
        val refreshToken = jwtTokenProvider.createRefreshToken(memberId)
        redisRepository.save(
            REFRESH_TOKEN_KEY_PREFIX + memberId,
            refreshToken,
            Duration.ofMillis(jwtProperties.refreshTokenValidityMs),
        )
        return AuthTokenResponse(
            accessToken = jwtTokenProvider.createAccessToken(
                memberId,
                email,
                joinProvider,
                Role.USER
            ),
            refreshToken = refreshToken,
        )
    }

    // refresh 토큰으로 재발급 (rotation). Redis에 저장된 최신 토큰과 다르면 이미 폐기된 토큰의 재사용으로 간주해 세션을 강제 종료한다
    @Transactional(readOnly = true)
    fun reissue(refreshToken: String): AuthTokenResponse {
        if (!jwtTokenProvider.isValid(refreshToken)) {
            throw ApiErrorException(ResponseCodeEnum.INVALID_JWT_TOKEN)
        }
        val memberId = jwtTokenProvider.getMemberId(refreshToken)

        val savedRefreshToken = redisRepository.find(REFRESH_TOKEN_KEY_PREFIX + memberId)
        if (refreshToken != savedRefreshToken) {
            redisRepository.delete(REFRESH_TOKEN_KEY_PREFIX + memberId)
            throw ApiErrorException(ResponseCodeEnum.INVALID_JWT_TOKEN)
        }

        val member = memberRepository.findByIdOrNull(memberId)
            ?: throw ApiErrorException(ResponseCodeEnum.NOT_FOUND_USER)
        return issue(member.id, member.email, member.joinProvider)
    }
}
