package com.example.springbootkotlinpractice.domain.auth.api

import com.example.springbootkotlinpractice.domain.auth.dto.AuthTokenResponse
import com.example.springbootkotlinpractice.domain.auth.dto.EmailLoginRequest
import com.example.springbootkotlinpractice.domain.auth.dto.EmailSignUpRequest
import com.example.springbootkotlinpractice.domain.auth.service.AuthService
import com.example.springbootkotlinpractice.common.dto.CommonResponse
import com.example.springbootkotlinpractice.common.dto.ResponseHandler
import com.example.springbootkotlinpractice.enums.JoinProvider
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api/v1/auth/email")
@RestController
class AuthEmailController(
    private val authService: AuthService,
) {
    @PostMapping("/sign-up")
    fun insertEmailMember(
        @RequestBody @Valid request: EmailSignUpRequest
    ): ResponseEntity<CommonResponse<AuthTokenResponse>> {
        return ResponseHandler.created(
            authService.signUp(request, JoinProvider.EMAIL)
        )
    }

    @PostMapping("/login")
    fun login(
        @RequestBody @Valid request: EmailLoginRequest
    ): ResponseEntity<CommonResponse<AuthTokenResponse>> {
        return ResponseHandler.ok(
            authService.login(request)
        )
    }

}
