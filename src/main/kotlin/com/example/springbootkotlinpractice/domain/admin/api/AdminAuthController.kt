package com.example.springbootkotlinpractice.domain.admin.api

import com.example.springbootkotlinpractice.common.dto.CommonResponse
import com.example.springbootkotlinpractice.common.dto.ResponseHandler
import com.example.springbootkotlinpractice.domain.admin.dto.AdminLoginRequest
import com.example.springbootkotlinpractice.domain.admin.service.AdminAuthService
import com.example.springbootkotlinpractice.domain.auth.dto.AuthTokenResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "[ADMIN] Auth", description = "관리자 로그인 API")
@RequestMapping("/api/v1/admin/auth")
@RestController
class AdminAuthController(
    private val adminAuthService: AdminAuthService,
) {

    @Operation(
        summary = "관리자 로그인 API",
        description = "이메일/비밀번호로 로그인 후 Access/Refresh Token을 발급한다.",
    )
    @PostMapping("/login")
    fun login(
        @RequestBody @Valid request: AdminLoginRequest,
    ): ResponseEntity<CommonResponse<AuthTokenResponse>> {
        return ResponseHandler.ok(adminAuthService.login(request))
    }
}
