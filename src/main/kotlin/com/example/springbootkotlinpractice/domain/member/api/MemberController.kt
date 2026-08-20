package com.example.springbootkotlinpractice.domain.member.api

import com.example.springbootkotlinpractice.common.dto.CommonResponse
import com.example.springbootkotlinpractice.common.dto.ResponseHandler
import com.example.springbootkotlinpractice.common.security.UserPrincipal
import com.example.springbootkotlinpractice.domain.member.dto.MemberResponse
import com.example.springbootkotlinpractice.domain.member.entity.Member
import com.example.springbootkotlinpractice.domain.member.service.MemberService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "[MEMBER] Member", description = "회원 조회 API")
@RequestMapping("/api/v1/members")
@RestController
class MemberController(
    private val memberService: MemberService,
) {
    @Operation(
        summary = "내 정보 조회 API",
        description = "로그인한 회원 자신의 정보를 조회한다.",
    )
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/myself")
    fun getMyself(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
    ): ResponseEntity<CommonResponse<MemberResponse>> {
        val member: Member = memberService.getMember(userPrincipal.id)
        return ResponseHandler.ok(
            MemberResponse(
                id = member.id,
                uuid = member.uuid,
                lastName = member.lastName,
                firstName = member.firstName,
                birthDate = member.birthDate,
                phoneNumber = member.phoneNumber,
                email = member.email,
                joinProvider = member.joinProvider
            )
        )
    }

//    @GetMapping("/myself-admin")
//    fun getMyselfAdmin(
//        @AuthenticationPrincipal userPrincipal: UserPrincipal,
//    ): ResponseEntity<CommonResponse<MemberResponse>> {
//        val member: Member = memberService.getMember(userPrincipal.id)
//        return ResponseHandler.ok(
//            MemberResponse(
//                id = member.id,
//                uuid = member.uuid,
//                lastName = member.lastName,
//                firstName = member.firstName,
//                birthDate = member.birthDate,
//                phoneNumber = member.phoneNumber,
//                email = member.email,
//                joinProvider = member.joinProvider
//            )
//        )
//    }
}