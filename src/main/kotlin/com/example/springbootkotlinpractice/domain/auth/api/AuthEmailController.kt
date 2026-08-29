package com.example.springbootkotlinpractice.domain.auth.api

import com.example.springbootkotlinpractice.common.dto.CommonResponse
import com.example.springbootkotlinpractice.common.dto.ResponseHandler
import com.example.springbootkotlinpractice.domain.auth.dto.AuthTokenResponse
import com.example.springbootkotlinpractice.domain.auth.dto.EmailLoginRequest
import com.example.springbootkotlinpractice.domain.auth.dto.EmailLoginResponse
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
        description = "Email 계정으로 1차 인증한다. TOTP 미사용 회원은 status=LOGIN과 함께 즉시 " +
                "accessToken/refreshToken을 발급하고, TOTP 사용 회원은 status=NEED_TOTP와 함께 " +
                "totpPendingToken만 발급한다(최종 토큰은 /api/v1/auth/totp/login에서 발급).",
    )
    @PostMapping("/login")
    fun login(
        @RequestBody @Valid request: EmailLoginRequest
    ): ResponseEntity<CommonResponse<EmailLoginResponse>> {
        return ResponseHandler.ok(
            authService.login(request)
        )
    }

}
