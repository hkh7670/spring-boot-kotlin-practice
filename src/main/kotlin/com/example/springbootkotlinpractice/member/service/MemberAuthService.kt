package com.example.springbootkotlinpractice.member.service

import com.example.springbootkotlinpractice.common.oauth.OAuthClientResolver
import com.example.springbootkotlinpractice.common.security.JwtTokenProvider
import com.example.springbootkotlinpractice.enums.JoinProvider
import com.example.springbootkotlinpractice.enums.ResponseCodeEnum
import com.example.springbootkotlinpractice.enums.Role
import com.example.springbootkotlinpractice.exception.ApiErrorException
import com.example.springbootkotlinpractice.member.dto.MemberCreateRequest
import com.example.springbootkotlinpractice.member.dto.MemberTokenResponse
import com.example.springbootkotlinpractice.member.dto.OAuthLoginResponse
import com.example.springbootkotlinpractice.member.dto.OAuthLoginStatus
import com.example.springbootkotlinpractice.member.dto.OAuthSignUpRequest
import com.example.springbootkotlinpractice.member.entity.Member
import com.example.springbootkotlinpractice.member.repository.MemberRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class MemberAuthService(
    private val memberRepository: MemberRepository,
    private val jwtTokenProvider: JwtTokenProvider,
    private val oAuthClientResolver: OAuthClientResolver,
) {

    // 가입경로에 맞춰 회원 생성 후 토큰 발급
    fun signUp(request: MemberCreateRequest, joinProvider: JoinProvider): MemberTokenResponse {
        if (memberRepository.existsByEmailAndJoinProvider(request.email, joinProvider)) {
            throw ApiErrorException(ResponseCodeEnum.DUPLICATED_EMAIL)
        }
        val member = memberRepository.save(
            Member.of(
                lastName = request.lastName,
                firstName = request.firstName,
                age = request.age,
                phoneNumber = request.phoneNumber,
                email = request.email,
                joinProvider = joinProvider,
            )
        )
        return issue(member.id, member.joinProvider)
    }

    // OAuth Provider 토큰으로 기존 회원이면 JWT 발급, 신규 회원이면 tempToken 발급
    fun oauthLogin(provider: JoinProvider, accessToken: String): OAuthLoginResponse {
        val userInfo = oAuthClientResolver.resolve(provider).getUserInfo(accessToken)
        val member = memberRepository.findByProviderIdAndJoinProvider(userInfo.providerId, provider)

        return if (member != null) {
            val tokens = issue(member.id, member.joinProvider)
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
    fun oauthSignUp(request: OAuthSignUpRequest): MemberTokenResponse {
        val claims = jwtTokenProvider.parseTempToken(request.tempToken)

        if (memberRepository.findByProviderIdAndJoinProvider(claims.providerId, claims.provider) != null) {
            throw ApiErrorException(ResponseCodeEnum.ALREADY_REGISTERED_OAUTH)
        }

        val member = memberRepository.save(
            Member.ofOAuth(
                providerId = claims.providerId,
                email = claims.email,
                lastName = request.lastName,
                firstName = request.firstName,
                age = request.age,
                phoneNumber = request.phoneNumber,
                joinProvider = claims.provider,
            )
        )
        return issue(member.id, member.joinProvider)
    }

    // access + refresh 동시 발급
    fun issue(memberId: Long, joinProvider: JoinProvider): MemberTokenResponse {
        return MemberTokenResponse(
            accessToken = jwtTokenProvider.createAccessToken(memberId, joinProvider, Role.USER),
            refreshToken = jwtTokenProvider.createRefreshToken(memberId),
        )
    }

    // refresh 토큰으로 재발급
    @Transactional(readOnly = true)
    fun reissue(refreshToken: String): MemberTokenResponse {
        if (!jwtTokenProvider.isValid(refreshToken)) {
            throw ApiErrorException(ResponseCodeEnum.INVALID_JWT_TOKEN)
        }
        val memberId = jwtTokenProvider.getMemberId(refreshToken)
        val member = memberRepository.findByIdOrNull(memberId)
            ?: throw ApiErrorException(ResponseCodeEnum.NOT_FOUND_USER)
        return issue(member.id, member.joinProvider)
    }
}
