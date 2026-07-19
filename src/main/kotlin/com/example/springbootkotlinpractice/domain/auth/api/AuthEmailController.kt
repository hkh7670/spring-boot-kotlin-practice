package com.example.springbootkotlinpractice.domain.auth.api

import com.example.springbootkotlinpractice.common.dto.CommonResponse
import com.example.springbootkotlinpractice.common.dto.ResponseHandler
import com.example.springbootkotlinpractice.domain.auth.dto.AuthTokenResponse
import com.example.springbootkotlinpractice.domain.auth.dto.EmailLoginRequest
import com.example.springbootkotlinpractice.domain.auth.dto.EmailSignUpRequest
import com.example.springbootkotlinpractice.domain.auth.service.AuthService
import com.example.springbootkotlinpractice.enums.JoinProvider
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "[EMAIL] Auth ", description = "Email 로그인 / 회원가입 API")
@RequestMapping("/api/v1/auth/email")
@RestController
class AuthEmailController(
    private val authService: AuthService,
) {
    @Operation(
        summary = "Email 회원가입 API",
        description = "Email 회원가입 API",
    )
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "처리 성공"),
    )
    @PostMapping("/sign-up")
    fun insertEmailMember(
        @RequestBody @Valid request: EmailSignUpRequest
    ): ResponseEntity<CommonResponse<AuthTokenResponse>> {
        return ResponseHandler.created(
            authService.signUp(request, JoinProvider.EMAIL)
        )
    }

    @Operation(
        summary = "Email 로그인 API",
        description = "Email 계정의 accessToken과 refreshToken을 발급한다.",
    )
    @PostMapping("/login")
    fun login(
        @RequestBody @Valid request: EmailLoginRequest
    ): ResponseEntity<CommonResponse<AuthTokenResponse>> {
        return ResponseHandler.ok(
            authService.login(request)
        )
    }

}
