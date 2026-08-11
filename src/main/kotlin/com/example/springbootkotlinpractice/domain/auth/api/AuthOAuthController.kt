package com.example.springbootkotlinpractice.domain.auth.api

import com.example.springbootkotlinpractice.common.Logging
import com.example.springbootkotlinpractice.common.dto.CommonResponse
import com.example.springbootkotlinpractice.common.dto.ResponseHandler
import com.example.springbootkotlinpractice.domain.auth.dto.AuthTokenResponse
import com.example.springbootkotlinpractice.domain.auth.dto.OAuthExchangeRequest
import com.example.springbootkotlinpractice.domain.auth.dto.OAuthLoginResponse
import com.example.springbootkotlinpractice.domain.auth.dto.OAuthSignUpRequest
import com.example.springbootkotlinpractice.domain.auth.service.AuthService
import com.example.springbootkotlinpractice.domain.auth.service.OAuthRelayCodeService
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


@Tag(name = "[OAuth] Auth", description = "OAuth 로그인(oauth2Login) 결과 교환 / 회원가입 API")
@RequestMapping("/api/v1/auth/oauth")
@RestController
class AuthOAuthController(
    private val authService: AuthService,
    private val oAuthRelayCodeService: OAuthRelayCodeService,
) : Logging {

    @Operation(
        summary = "OAuth 로그인 결과 교환",
        description = "Google/Kakao/Naver 로그인(oauth2Login) 콜백 처리 후 프론트로 리다이렉트될 때 실린 " +
                "1회용 relay code로 실제 로그인 결과(LOGIN 상태의 JWT 또는 NEED_SIGN_UP 상태의 tempToken)를 조회한다. " +
                "code는 1회만 사용 가능하며 조회 즉시 만료된다.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "처리 성공 (LOGIN 또는 NEED_SIGN_UP)"),
        ApiResponse(responseCode = "400", description = "존재하지 않거나 이미 사용됐거나 만료된 code"),
    )
    @PostMapping("/exchange")
    fun exchange(
        @RequestBody @Valid request: OAuthExchangeRequest,
    ): ResponseEntity<CommonResponse<OAuthLoginResponse>> {
        return ResponseHandler.ok(oAuthRelayCodeService.consume(request.code))
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
    ): ResponseEntity<CommonResponse<AuthTokenResponse>> {
        return ResponseHandler.ok(
            authService.oauthSignUp(request)
        )
    }
}
