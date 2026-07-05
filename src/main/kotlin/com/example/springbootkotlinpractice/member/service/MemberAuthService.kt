package com.example.springbootkotlinpractice.member.service

import com.example.springbootkotlinpractice.common.oauth.OAuthClientResolver
import com.example.springbootkotlinpractice.common.security.JwtTokenProvider
import com.example.springbootkotlinpractice.enums.JoinProvider
import com.example.springbootkotlinpractice.enums.ResponseCodeEnum
import com.example.springbootkotlinpractice.enums.Role
import com.example.springbootkotlinpractice.exception.ApiErrorException
import com.example.springbootkotlinpractice.member.dto.MemberCreateRequest
import com.example.springbootkotlinpractice.member.dto.MemberTokenResponse
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
    @Transactional
    fun signUp(request: MemberCreateRequest, joinProvider: JoinProvider): MemberTokenResponse {
        if (memberRepository.existsByEmail(request.email)) {
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

    // OAuth Provider 토큰을 검증해 회원을 조회하고, 최초 로그인이면 자동 가입 후 토큰을 발급한다
    fun oauthLogin(provider: JoinProvider, accessToken: String): MemberTokenResponse {
        val userInfo = oAuthClientResolver.resolve(provider).getUserInfo(accessToken)
        val member = memberRepository.findByEmail(userInfo.email)
            ?: memberRepository.save(Member.ofOAuth(userInfo.email, userInfo.nickname, provider))
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
