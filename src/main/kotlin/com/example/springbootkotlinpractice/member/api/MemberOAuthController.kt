package com.example.springbootkotlinpractice.member.api

import com.example.springbootkotlinpractice.common.dto.CommonResponse
import com.example.springbootkotlinpractice.common.dto.ResponseHandler
import com.example.springbootkotlinpractice.enums.JoinProvider
import com.example.springbootkotlinpractice.member.dto.MemberTokenResponse
import com.example.springbootkotlinpractice.member.dto.OAuthLoginRequest
import com.example.springbootkotlinpractice.member.dto.OAuthLoginResponse
import com.example.springbootkotlinpractice.member.dto.OAuthSignUpRequest
import com.example.springbootkotlinpractice.member.service.MemberAuthService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api/v1/members/oauth")
@RestController
class MemberOAuthController(
    private val memberAuthService: MemberAuthService,
) {
    @PostMapping("/google")
    fun loginWithGoogle(
        @RequestBody @Valid request: OAuthLoginRequest,
    ): ResponseEntity<CommonResponse<OAuthLoginResponse>> {
        return ResponseHandler.ok(
            memberAuthService.oauthLogin(JoinProvider.GOOGLE, request.accessToken)
        )
    }

    @PostMapping("/kakao")
    fun loginWithKakao(
        @RequestBody @Valid request: OAuthLoginRequest,
    ): ResponseEntity<CommonResponse<OAuthLoginResponse>> {
        return ResponseHandler.ok(
            memberAuthService.oauthLogin(JoinProvider.KAKAO, request.accessToken)
        )
    }

    @PostMapping("/naver")
    fun loginWithNaver(
        @RequestBody @Valid request: OAuthLoginRequest,
    ): ResponseEntity<CommonResponse<OAuthLoginResponse>> {
        return ResponseHandler.ok(
            memberAuthService.oauthLogin(JoinProvider.NAVER, request.accessToken)
        )
    }

    @PostMapping("/sign-up")
    fun oauthSignUp(
        @RequestBody @Valid request: OAuthSignUpRequest,
    ): ResponseEntity<CommonResponse<MemberTokenResponse>> {
        return ResponseHandler.ok(
            memberAuthService.oauthSignUp(request)
        )
    }
}
