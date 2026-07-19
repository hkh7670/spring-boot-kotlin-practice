package com.example.springbootkotlinpractice.member.api

import com.example.springbootkotlinpractice.common.oauth.OAuthClient
import com.example.springbootkotlinpractice.common.oauth.OAuthClientResolver
import com.example.springbootkotlinpractice.common.oauth.OAuthUserInfo
import com.example.springbootkotlinpractice.common.redis.RedisRepository
import com.example.springbootkotlinpractice.enums.JoinProvider
import com.example.springbootkotlinpractice.member.dto.OAuthSignUpRequest
import com.example.springbootkotlinpractice.member.repository.MemberRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import java.time.LocalDate

private const val BASE_URL = "/api/v1/members/oauth"
private const val FAKE_CODE = "fake-auth-code"
private const val FAKE_CODE_VERIFIER = "fake-code-verifier"
private const val FAKE_REDIRECT_URI = "com.example.app:/oauth2redirect"

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("OAuth 로그인/회원가입 통합 테스트")
class MemberOAuthControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var memberRepository: MemberRepository

    @MockitoBean
    lateinit var oAuthClientResolver: OAuthClientResolver

    @MockitoBean
    lateinit var redisRepository: RedisRepository

    @BeforeEach
    fun setUp() {
        memberRepository.deleteAll()
    }

    // ── 공통 헬퍼 ──────────────────────────────────────────────────────────

    private fun stubOAuthClient(provider: JoinProvider, userInfo: OAuthUserInfo): OAuthClient {
        val mockClient = mock(OAuthClient::class.java)
        given(mockClient.getUserInfoByAuthorizationCode(FAKE_CODE, FAKE_CODE_VERIFIER, FAKE_REDIRECT_URI))
            .willReturn(userInfo)
        given(oAuthClientResolver.resolve(provider)).willReturn(mockClient)
        return mockClient
    }

    private fun oauthLoginRequestBody(): String {
        return """{"code": "$FAKE_CODE", "codeVerifier": "$FAKE_CODE_VERIFIER", "redirectUri": "$FAKE_REDIRECT_URI"}"""
    }

    private fun oauthLogin(endpoint: String): String {
        val result = mockMvc.post("$BASE_URL/$endpoint") {
            contentType = MediaType.APPLICATION_JSON
            content = oauthLoginRequestBody()
        }.andReturn()
        return result.response.contentAsString
    }

    private fun extractTempToken(responseBody: String): String {
        return objectMapper.readTree(responseBody)["data"]["tempToken"].asText()
    }

    private fun signUp(tempToken: String): String {
        val request = OAuthSignUpRequest(
            tempToken = tempToken,
            lastName = "한",
            firstName = "규호",
            birthDate = LocalDate.of(1998, 5, 20),
            phoneNumber = "010-1234-5678",
        )
        val result = mockMvc.post("$BASE_URL/sign-up") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andReturn()
        return result.response.contentAsString
    }

    // ── Google ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Google OAuth")
    inner class GoogleOAuth {

        private val googleUser = OAuthUserInfo(
            providerId = "google-123456789",
            email = "testuser@gmail.com",
            nickname = null,
        )

        @BeforeEach
        fun stubGoogle() {
            stubOAuthClient(JoinProvider.GOOGLE, googleUser)
        }

        @Test
        @DisplayName("신규 유저 - NEED_SIGN_UP + tempToken 반환")
        fun `Google 신규 유저 로그인 시 NEED_SIGN_UP 상태와 tempToken을 반환한다`() {
            mockMvc.post("$BASE_URL/google") {
                contentType = MediaType.APPLICATION_JSON
                content = oauthLoginRequestBody()
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.status") { value("NEED_SIGN_UP") }
                jsonPath("$.data.tempToken") { isNotEmpty() }
                jsonPath("$.data.accessToken") { doesNotExist() }
                jsonPath("$.data.refreshToken") { doesNotExist() }
            }
        }

        @Test
        @DisplayName("신규 유저 - tempToken으로 회원가입 후 JWT 발급")
        fun `Google tempToken으로 회원가입 시 JWT 토큰을 반환하고 Member가 저장된다`() {
            val tempToken = extractTempToken(oauthLogin("google"))

            val signUpBody = signUp(tempToken)
            val tree = objectMapper.readTree(signUpBody)

            assertThat(tree["data"]["accessToken"].asText()).isNotBlank()
            assertThat(tree["data"]["refreshToken"].asText()).isNotBlank()

            val saved = memberRepository.findByProviderIdAndJoinProvider(googleUser.providerId, JoinProvider.GOOGLE)
            assertThat(saved).isNotNull
            assertThat(saved!!.email).isEqualTo(googleUser.email)
            assertThat(saved.joinProvider).isEqualTo(JoinProvider.GOOGLE)
        }

        @Test
        @DisplayName("기존 유저 - LOGIN + JWT 발급")
        fun `Google 기존 유저 로그인 시 LOGIN 상태와 JWT를 반환한다`() {
            val tempToken = extractTempToken(oauthLogin("google"))
            signUp(tempToken)

            mockMvc.post("$BASE_URL/google") {
                contentType = MediaType.APPLICATION_JSON
                content = oauthLoginRequestBody()
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.status") { value("LOGIN") }
                jsonPath("$.data.accessToken") { isNotEmpty() }
                jsonPath("$.data.refreshToken") { isNotEmpty() }
                jsonPath("$.data.tempToken") { doesNotExist() }
            }
        }

        @Test
        @DisplayName("이미 가입된 providerId로 재가입 시도 - 에러 반환")
        fun `Google 중복 회원가입 시도 시 에러를 반환한다`() {
            val tempToken = extractTempToken(oauthLogin("google"))
            signUp(tempToken)

            mockMvc.post("$BASE_URL/sign-up") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    OAuthSignUpRequest(
                        tempToken = tempToken,
                        lastName = "한",
                        firstName = "규호",
                        birthDate = LocalDate.of(1998, 5, 20),
                        phoneNumber = "010-9999-9999",
                    )
                )
            }.andExpect {
                status { isConflict() }
            }
        }
    }

    // ── Kakao ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Kakao OAuth")
    inner class KakaoOAuth {

        private val kakaoUser = OAuthUserInfo(
            providerId = "kakao-9876543210",
            email = "testuser@kakao.com",
            nickname = "카카오테스터",
        )

        @BeforeEach
        fun stubKakao() {
            stubOAuthClient(JoinProvider.KAKAO, kakaoUser)
        }

        @Test
        @DisplayName("신규 유저 - NEED_SIGN_UP + tempToken 반환 (email, nickname 포함)")
        fun `Kakao 신규 유저 로그인 시 NEED_SIGN_UP 상태와 tempToken을 반환한다`() {
            mockMvc.post("$BASE_URL/kakao") {
                contentType = MediaType.APPLICATION_JSON
                content = oauthLoginRequestBody()
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.status") { value("NEED_SIGN_UP") }
                jsonPath("$.data.tempToken") { isNotEmpty() }
            }
        }

        @Test
        @DisplayName("신규 유저 - tempToken으로 회원가입 후 JWT 발급")
        fun `Kakao tempToken으로 회원가입 시 JWT 토큰을 반환하고 Member가 저장된다`() {
            val tempToken = extractTempToken(oauthLogin("kakao"))

            val signUpBody = signUp(tempToken)
            val tree = objectMapper.readTree(signUpBody)

            assertThat(tree["data"]["accessToken"].asText()).isNotBlank()

            val saved = memberRepository.findByProviderIdAndJoinProvider(kakaoUser.providerId, JoinProvider.KAKAO)
            assertThat(saved).isNotNull
            assertThat(saved!!.email).isEqualTo(kakaoUser.email)
            assertThat(saved.joinProvider).isEqualTo(JoinProvider.KAKAO)
        }

        @Test
        @DisplayName("기존 유저 - LOGIN + JWT 발급")
        fun `Kakao 기존 유저 로그인 시 LOGIN 상태와 JWT를 반환한다`() {
            val tempToken = extractTempToken(oauthLogin("kakao"))
            signUp(tempToken)

            mockMvc.post("$BASE_URL/kakao") {
                contentType = MediaType.APPLICATION_JSON
                content = oauthLoginRequestBody()
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.status") { value("LOGIN") }
                jsonPath("$.data.accessToken") { isNotEmpty() }
            }
        }
    }

    // ── Naver ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Naver OAuth")
    inner class NaverOAuth {

        private val naverUser = OAuthUserInfo(
            providerId = "naver-ABCDE12345",
            email = null,
            nickname = "네이버테스터",
        )

        @BeforeEach
        fun stubNaver() {
            stubOAuthClient(JoinProvider.NAVER, naverUser)
        }

        @Test
        @DisplayName("신규 유저 - email null이어도 NEED_SIGN_UP + tempToken 반환")
        fun `Naver 신규 유저 로그인 시 이메일 없어도 NEED_SIGN_UP 상태와 tempToken을 반환한다`() {
            mockMvc.post("$BASE_URL/naver") {
                contentType = MediaType.APPLICATION_JSON
                content = oauthLoginRequestBody()
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.status") { value("NEED_SIGN_UP") }
                jsonPath("$.data.tempToken") { isNotEmpty() }
            }
        }

        @Test
        @DisplayName("신규 유저 - email null인 상태로 회원가입 후 JWT 발급")
        fun `Naver tempToken으로 회원가입 시 email이 null이어도 Member가 저장된다`() {
            val tempToken = extractTempToken(oauthLogin("naver"))

            val signUpBody = signUp(tempToken)
            val tree = objectMapper.readTree(signUpBody)

            assertThat(tree["data"]["accessToken"].asText()).isNotBlank()

            val saved = memberRepository.findByProviderIdAndJoinProvider(naverUser.providerId, JoinProvider.NAVER)
            assertThat(saved).isNotNull
            assertThat(saved!!.email).isNull()
            assertThat(saved.joinProvider).isEqualTo(JoinProvider.NAVER)
        }

        @Test
        @DisplayName("기존 유저 - LOGIN + JWT 발급")
        fun `Naver 기존 유저 로그인 시 LOGIN 상태와 JWT를 반환한다`() {
            val tempToken = extractTempToken(oauthLogin("naver"))
            signUp(tempToken)

            mockMvc.post("$BASE_URL/naver") {
                contentType = MediaType.APPLICATION_JSON
                content = oauthLoginRequestBody()
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.status") { value("LOGIN") }
                jsonPath("$.data.accessToken") { isNotEmpty() }
            }
        }
    }

    // ── 공통 에러 케이스 ────────────────────────────────────────────────────

    @Nested
    @DisplayName("공통 에러 케이스")
    inner class CommonErrorCases {

        @Test
        @DisplayName("유효하지 않은 tempToken으로 회원가입 시도 - 401 반환")
        fun `유효하지 않은 tempToken으로 회원가입 시 에러를 반환한다`() {
            mockMvc.post("$BASE_URL/sign-up") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    OAuthSignUpRequest(
                        tempToken = "invalid.jwt.token",
                        lastName = "한",
                        firstName = "규호",
                        birthDate = LocalDate.of(1998, 5, 20),
                        phoneNumber = "010-1234-5678",
                    )
                )
            }.andExpect {
                status { isUnauthorized() }
            }
        }

        @Test
        @DisplayName("code/codeVerifier/redirectUri 빈 값으로 Kakao 로그인 요청 시 - 유효성 검사 에러")
        fun `필수 필드가 빈 값이면 Kakao 로그인 요청 시 유효성 검사 에러를 반환한다`() {
            mockMvc.post("$BASE_URL/kakao") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"code": "", "codeVerifier": "", "redirectUri": ""}"""
            }.andExpect {
                status { isBadRequest() }
            }
        }

        @Test
        @DisplayName("code/codeVerifier/redirectUri 빈 값으로 Naver 로그인 요청 시 - 유효성 검사 에러")
        fun `필수 필드가 빈 값이면 Naver 로그인 요청 시 유효성 검사 에러를 반환한다`() {
            mockMvc.post("$BASE_URL/naver") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"code": "", "codeVerifier": "", "redirectUri": ""}"""
            }.andExpect {
                status { isBadRequest() }
            }
        }

        @Test
        @DisplayName("code/codeVerifier/redirectUri 빈 값으로 Google 로그인 요청 시 - 유효성 검사 에러")
        fun `필수 필드가 빈 값이면 Google 로그인 요청 시 유효성 검사 에러를 반환한다`() {
            mockMvc.post("$BASE_URL/google") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"code": "", "codeVerifier": "", "redirectUri": ""}"""
            }.andExpect {
                status { isBadRequest() }
            }
        }
    }
}
