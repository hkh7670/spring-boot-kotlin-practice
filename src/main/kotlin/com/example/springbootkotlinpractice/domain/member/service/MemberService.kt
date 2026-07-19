package com.example.springbootkotlinpractice.domain.member.service

import com.example.springbootkotlinpractice.enums.ResponseCodeEnum
import com.example.springbootkotlinpractice.exception.ApiErrorException
import com.example.springbootkotlinpractice.domain.member.entity.Member
import com.example.springbootkotlinpractice.domain.member.repository.MemberRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class MemberService(
    private val memberRepository: MemberRepository
) {
    fun getMember(id: Long): Member {
        return memberRepository.findByIdOrNull(id)
            ?: throw ApiErrorException(ResponseCodeEnum.NOT_FOUND_USER)
    }

}