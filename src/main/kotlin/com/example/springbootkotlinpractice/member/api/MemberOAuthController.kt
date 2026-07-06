package com.example.springbootkotlinpractice.member.api

import com.example.springbootkotlinpractice.common.dto.CommonResponse
import com.example.springbootkotlinpractice.common.dto.ResponseHandler
import com.example.springbootkotlinpractice.enums.JoinProvider
import com.example.springbootkotlinpractice.member.dto.*
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

@Tag(name = "Member OAuth", description = "OAuth 로그인 / 회원가입 API")
@RequestMapping("/api/v1/members/oauth")
@RestController
class MemberOAuthController(
    private val memberAuthService: MemberAuthService,
) {
    @Operation(
        summary = "Google 로그인",
        description = "Authorization Code + PKCE 로 Google Access Token 을 교환한 뒤, " +
                "기존 회원이면 LOGIN 상태와 JWT를, 신규 회원이면 NEED_SIGN_UP 상태와 tempToken 을 반환한다.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "처리 성공 (LOGIN 또는 NEED_SIGN_UP)"),
        ApiResponse(responseCode = "400", description = "code/codeVerifier/redirectUri 검증 실패"),
        ApiResponse(responseCode = "401", description = "유효하지 않은 code 이거나 PKCE(code_verifier) 검증 실패"),
        ApiResponse(responseCode = "500", description = "Google API 호출 중 오류 발생"),
    )
    @PostMapping("/google")
    fun loginWithGoogle(
        @RequestBody @Valid request: GoogleOAuthLoginRequest,
    ): ResponseEntity<CommonResponse<OAuthLoginResponse>> {
        return ResponseHandler.ok(
            memberAuthService.oauthLoginWithGoogleCode(
                request.code,
                request.codeVerifier,
                request.redirectUri
            )
        )
    }

    @Operation(
        summary = "Kakao 로그인",
        description = "클라이언트가 발급받은 Kakao Access Token 으로 로그인한다. " +
                "기존 회원이면 LOGIN 상태와 JWT를, 신규 회원이면 NEED_SIGN_UP 상태와 tempToken 을 반환한다.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "처리 성공 (LOGIN 또는 NEED_SIGN_UP)"),
        ApiResponse(responseCode = "400", description = "accessToken 검증 실패"),
        ApiResponse(responseCode = "401", description = "유효하지 않은 Kakao Access Token"),
        ApiResponse(responseCode = "500", description = "Kakao API 호출 중 오류 발생"),
    )
    @PostMapping("/kakao")
    fun loginWithKakao(
        @RequestBody @Valid request: OAuthLoginRequest,
    ): ResponseEntity<CommonResponse<OAuthLoginResponse>> {
        return ResponseHandler.ok(
            memberAuthService.oauthLogin(JoinProvider.KAKAO, request.accessToken)
        )
    }

    @Operation(
        summary = "Naver 로그인",
        description = "클라이언트가 발급받은 Naver Access Token 으로 로그인한다. " +
                "기존 회원이면 LOGIN 상태와 JWT를, 신규 회원이면 NEED_SIGN_UP 상태와 tempToken 을 반환한다.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "처리 성공 (LOGIN 또는 NEED_SIGN_UP)"),
        ApiResponse(responseCode = "400", description = "accessToken 검증 실패"),
        ApiResponse(responseCode = "401", description = "유효하지 않은 Naver Access Token"),
        ApiResponse(responseCode = "500", description = "Naver API 호출 중 오류 발생"),
    )
    @PostMapping("/naver")
    fun loginWithNaver(
        @RequestBody @Valid request: OAuthLoginRequest,
    ): ResponseEntity<CommonResponse<OAuthLoginResponse>> {
        return ResponseHandler.ok(
            memberAuthService.oauthLogin(JoinProvider.NAVER, request.accessToken)
        )
    }

    @Operation(
        summary = "OAuth 신규 회원가입",
        description = "로그인 응답으로 받은 tempToken 과 추가 정보(이름/나이/전화번호)로 회원가입을 완료하고 JWT를 발급한다.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "회원가입 성공, JWT 발급"),
        ApiResponse(responseCode = "400", description = "요청 필드 검증 실패"),
        ApiResponse(responseCode = "401", description = "유효하지 않거나 만료된 tempToken"),
        ApiResponse(responseCode = "409", description = "이미 가입된 OAuth 계정"),
    )
    @PostMapping("/sign-up")
    fun oauthSignUp(
        @RequestBody @Valid request: OAuthSignUpRequest,
    ): ResponseEntity<CommonResponse<MemberTokenResponse>> {
        return ResponseHandler.ok(
            memberAuthService.oauthSignUp(request)
        )
    }
}
