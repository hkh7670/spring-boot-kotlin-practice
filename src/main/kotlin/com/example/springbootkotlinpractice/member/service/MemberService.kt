package com.example.springbootkotlinpractice.member.service

import com.example.springbootkotlinpractice.enums.ResponseCodeEnum
import com.example.springbootkotlinpractice.exception.ApiErrorException
import com.example.springbootkotlinpractice.member.dto.MemberCreateRequest
import com.example.springbootkotlinpractice.member.entity.Member
import com.example.springbootkotlinpractice.member.repository.MemberRepository
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

    fun insertMember(
        request: MemberCreateRequest
    ): Member {
        return memberRepository.save<Member>(
            Member.of(
                lastName = request.lastName,
                firstName = request.firstName,
                age = request.age,
                phoneNumber = request.phoneNumber,
                email = request.email,
            )
        )
    }
}