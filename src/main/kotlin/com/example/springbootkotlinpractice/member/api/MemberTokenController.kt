package com.example.springbootkotlinpractice.member.api

import com.example.springbootkotlinpractice.common.dto.CommonResponse
import com.example.springbootkotlinpractice.common.dto.ResponseHandler
import com.example.springbootkotlinpractice.member.dto.MemberTokenResponse
import com.example.springbootkotlinpractice.member.dto.RefreshTokenRequest
import com.example.springbootkotlinpractice.member.service.MemberAuthService
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

@Tag(name = "Member Token", description = "Access/Refresh Token 재발급 API")
@RequestMapping("/api/v1/members")
@RestController
class MemberTokenController(
    private val memberAuthService: MemberAuthService,
) {
    @Operation(
        summary = "Access Token 재발급",
        description = "Refresh Token으로 Access/Refresh Token을 재발급한다. EMAIL/OAuth(Google/Kakao/Naver) 가입 회원 공통으로 사용 가능하다.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "재발급 성공"),
        ApiResponse(responseCode = "401", description = "유효하지 않거나 만료된 Refresh Token"),
        ApiResponse(responseCode = "404", description = "회원정보가 존재하지 않음"),
    )
    @PostMapping("/reissue")
    fun reissue(
        @RequestBody @Valid request: RefreshTokenRequest,
    ): ResponseEntity<CommonResponse<MemberTokenResponse>> {
        return ResponseHandler.ok(
            memberAuthService.reissue(request.refreshToken)
        )
    }
}
