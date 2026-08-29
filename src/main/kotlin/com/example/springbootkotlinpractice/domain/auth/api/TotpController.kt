package com.example.springbootkotlinpractice.domain.auth.api

import com.example.springbootkotlinpractice.common.dto.CommonResponse
import com.example.springbootkotlinpractice.common.dto.ResponseHandler
import com.example.springbootkotlinpractice.common.security.UserPrincipal
import com.example.springbootkotlinpractice.domain.auth.dto.AuthTokenResponse
import com.example.springbootkotlinpractice.domain.auth.dto.TotpEnrollResponse
import com.example.springbootkotlinpractice.domain.auth.dto.TotpLoginRequest
import com.example.springbootkotlinpractice.domain.auth.dto.TotpVerifyRequest
import com.example.springbootkotlinpractice.domain.auth.service.AuthService
import com.example.springbootkotlinpractice.domain.auth.service.TotpService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "[TOTP] Auth", description = "TOTP(OTP 앱) 2단계 인증 등록/해제/로그인 API")
@RequestMapping("/api/v1/auth/totp")
@RestController
class TotpController(
    private val authService: AuthService,
    private val totpService: TotpService,
) {

    @Operation(
        summary = "TOTP 2단계 인증 로그인 API",
        description = "Email 로그인에서 받은 totpPendingToken과 OTP 앱 코드로 최종 accessToken/refreshToken을 발급한다.",
    )
    @PostMapping("/login")
    fun login(
        @RequestBody @Valid request: TotpLoginRequest
    ): ResponseEntity<CommonResponse<AuthTokenResponse>> {
        return ResponseHandler.ok(
            authService.totpLogin(request)
        )
    }

    @Operation(
        summary = "TOTP 등록 시작 API",
        description = "새 시크릿을 발급해 5분간 Redis에 임시 보관한다. 반환된 otpAuthUri를 QR코드로 렌더링해 " +
                "OTP 앱에 등록하고, 첫 코드를 /enroll/confirm 으로 제출해야 실제로 활성화된다.",
    )
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/enroll")
    fun enroll(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
    ): ResponseEntity<CommonResponse<TotpEnrollResponse>> {
        return ResponseHandler.ok(
            totpService.enroll(userPrincipal.id)
        )
    }

    @Operation(
        summary = "TOTP 등록 확정 API",
        description = "enroll에서 발급된 시크릿에 대한 첫 코드를 검증하고, 성공 시 회원의 TOTP를 활성화한다.",
    )
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/enroll/confirm")
    fun confirmEnroll(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @RequestBody @Valid request: TotpVerifyRequest,
    ): ResponseEntity<CommonResponse<Unit>> {
        totpService.confirmEnroll(userPrincipal.id, request.code)
        return ResponseHandler.ok()
    }

    @Operation(
        summary = "TOTP 비활성화 API",
        description = "현재 유효한 TOTP 코드를 검증한 뒤 TOTP를 비활성화하고 시크릿을 삭제한다.",
    )
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/disable")
    fun disable(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @RequestBody @Valid request: TotpVerifyRequest,
    ): ResponseEntity<CommonResponse<Unit>> {
        totpService.disable(userPrincipal.id, request.code)
        return ResponseHandler.ok()
    }
}
