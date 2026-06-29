package com.example.springbootkotlinpractice.member.service

import com.example.springbootkotlinpractice.common.security.JwtTokenProvider
import com.example.springbootkotlinpractice.member.dto.MemberTokenResponse
import com.example.springbootkotlinpractice.enums.JoinProvider
import com.example.springbootkotlinpractice.enums.ResponseCodeEnum
import com.example.springbootkotlinpractice.exception.ApiErrorException
import com.example.springbootkotlinpractice.member.dto.MemberCreateRequest
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
) {

    // 가입경로에 맞춰 회원 생성 후 토큰 발급
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

    // access + refresh 동시 발급
    fun issue(memberId: Long, joinProvider: JoinProvider): MemberTokenResponse {
        return MemberTokenResponse(
            accessToken = jwtTokenProvider.createAccessToken(memberId, joinProvider),
            refreshToken = jwtTokenProvider.createRefreshToken(memberId),
        )
    }

    // refresh 토큰으로 재발급
    @Transactional(readOnly = true)
    fun reissue(refreshToken: String): MemberTokenResponse {
        if (!jwtTokenProvider.isValid(refreshToken)) {
            throw ApiErrorException(ResponseCodeEnum.INVALID_API_KEY)
        }
        val memberId = jwtTokenProvider.getMemberId(refreshToken)
        val member = memberRepository.findByIdOrNull(memberId)
            ?: throw ApiErrorException(ResponseCodeEnum.NOT_FOUND_USER)
        return issue(member.id, member.joinProvider)
    }
}
