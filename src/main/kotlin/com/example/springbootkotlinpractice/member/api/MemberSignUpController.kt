package com.example.springbootkotlinpractice.member.api

import com.example.springbootkotlinpractice.common.dto.CommonResponse
import com.example.springbootkotlinpractice.common.dto.ResponseHandler
import com.example.springbootkotlinpractice.enums.JoinProvider
import com.example.springbootkotlinpractice.member.dto.MemberCreateRequest
import com.example.springbootkotlinpractice.member.dto.MemberTokenResponse
import com.example.springbootkotlinpractice.member.service.MemberAuthService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api/v1/members/signup")
@RestController
class MemberSignUpController(
    private val memberAuthService: MemberAuthService,
) {
    @PostMapping("/email")
    fun insertEmailMember(
        @RequestBody @Valid request: MemberCreateRequest
    ): ResponseEntity<CommonResponse<MemberTokenResponse>> {
        return ResponseHandler.created(
            memberAuthService.signUp(request, JoinProvider.EMAIL)
        )
    }

    @PostMapping("/google")
    fun insertGoogleMember(
        @RequestBody @Valid request: MemberCreateRequest
    ): ResponseEntity<CommonResponse<MemberTokenResponse>> {
        return ResponseHandler.created(
            memberAuthService.signUp(request, JoinProvider.GOOGLE)
        )
    }
}