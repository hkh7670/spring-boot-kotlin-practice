package com.example.springbootkotlinpractice.member.api

import com.example.springbootkotlinpractice.common.dto.CommonResponse
import com.example.springbootkotlinpractice.common.dto.ResponseHandler
import com.example.springbootkotlinpractice.member.dto.MemberResponse
import com.example.springbootkotlinpractice.member.entity.Member
import com.example.springbootkotlinpractice.member.service.MemberService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api/v1/members")
@RestController
class MemberController(
    private val memberService: MemberService
) {
    @GetMapping("/{id}")
    fun getMember(
        @PathVariable id: Long
    ): ResponseEntity<CommonResponse<MemberResponse>> {
        val member: Member = memberService.getMember(id)
        return ResponseHandler.ok(
            MemberResponse(
                id = member.id,
                uuid = member.uuid,
                lastName = member.lastName,
                firstName = member.firstName,
                age = member.age,
                phoneNumber = member.phoneNumber,
                email = member.email,
            )
        )
    }
}