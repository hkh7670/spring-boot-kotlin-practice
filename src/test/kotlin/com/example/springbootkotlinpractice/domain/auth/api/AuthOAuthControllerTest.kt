package com.example.springbootkotlinpractice.domain.auth.api

import com.example.springbootkotlinpractice.common.oauth.OAuthUserInfo
import com.example.springbootkotlinpractice.common.redis.RedisRepository
import com.example.springbootkotlinpractice.domain.auth.dto.OAuthSignUpRequest
import com.example.springbootkotlinpractice.domain.auth.service.AuthService
import com.example.springbootkotlinpractice.domain.auth.service.OAuthRelayCodeService
import com.example.springbootkotlinpractice.domain.member.repository.MemberRepository
import com.example.springbootkotlinpractice.enums.JoinProvider
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import java.time.LocalDate

private const val BASE_URL = "/api/v1/auth/oauth"

// oauth2Login()의 Provider 리다이렉트 왕복 자체는 MockMvc로 흉내내기 어려워 다루지 않는다.
// 대신 우리가 직접 구현한 조각들 - AuthService.oauthLogin() 의 LOGIN/NEED_SIGN_UP 분기,
// relay code 교환(/exchange) 엔드포인트, tempToken 기반 회원가입(/sign-up) - 을 검증한다.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("OAuth 로그인 결과 교환 / 회원가입 통합 테스트")
class AuthOAuthControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var memberRepository: MemberRepository

    @Autowired
    lateinit var authService: AuthService

    @Autowired
    lateinit var oAuthRelayCodeService: OAuthRelayCodeService

    @MockitoBean
    lateinit var redisRepository: RedisRepository

    private val fakeRedisStore = mutableMapOf<String, String>()

    @BeforeEach
    fun setUp() {
        memberRepository.deleteAll()
        fakeRedisStore.clear()
        given(redisRepository.save(any(), any(), any())).willAnswer { invocation ->
            fakeRedisStore[invocation.getArgument(0)] = invocation.getArgument(1)
        }
        given(redisRepository.find(any())).willAnswer { invocation ->
            fakeRedisStore[invocation.getArgument<String>(0)]
        }
        given(redisRepository.delete(any())).willAnswer { invocation ->
            fakeRedisStore.remove(invocation.getArgument<String>(0)) != null
        }
    }

    // ── 공통 헬퍼 ──────────────────────────────────────────────────────────

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

    // ── /exchange ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /exchange")
    inner class Exchange {

        @Test
        @DisplayName("발급된 relay code로 로그인 결과(NEED_SIGN_UP)를 조회한다")
        fun `유효한 code면 relay code 발급 당시의 응답을 반환한다`() {
            val userInfo = OAuthUserInfo(
                providerId = "google-123456789",
                email = "testuser@gmail.com",
                nickname = null,
            )
            val loginResponse = authService.oauthLogin(JoinProvider.GOOGLE, userInfo)
            val code = oAuthRelayCodeService.issue(loginResponse)

            mockMvc.post("$BASE_URL/exchange") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"code": "$code"}"""
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.status") { value("NEED_SIGN_UP") }
                jsonPath("$.data.tempToken") { isNotEmpty() }
            }
        }

        @Test
        @DisplayName("같은 code를 두 번 사용하면 두 번째는 실패한다 (1회용)")
        fun `이미 소비된 code로 재요청하면 에러를 반환한다`() {
            val userInfo = OAuthUserInfo(providerId = "kakao-1", email = null, nickname = "닉네임")
            val code = oAuthRelayCodeService.issue(authService.oauthLogin(JoinProvider.KAKAO, userInfo))

            mockMvc.post("$BASE_URL/exchange") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"code": "$code"}"""
            }

            mockMvc.post("$BASE_URL/exchange") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"code": "$code"}"""
            }.andExpect {
                status { isBadRequest() }
            }
        }

        @Test
        @DisplayName("존재하지 않는 code로 요청하면 에러를 반환한다")
        fun `존재하지 않는 code면 에러를 반환한다`() {
            mockMvc.post("$BASE_URL/exchange") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"code": "no-such-code"}"""
            }.andExpect {
                status { isBadRequest() }
            }
        }

        @Test
        @DisplayName("code가 빈 값이면 유효성 검사 에러를 반환한다")
        fun `code가 빈 값이면 유효성 검사 에러를 반환한다`() {
            mockMvc.post("$BASE_URL/exchange") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"code": ""}"""
            }.andExpect {
                status { isBadRequest() }
            }
        }
    }

    // ── /sign-up ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /sign-up")
    inner class SignUp {

        @Test
        @DisplayName("신규 유저 - tempToken으로 회원가입 후 JWT 발급")
        fun `tempToken으로 회원가입 시 JWT 토큰을 반환하고 Member가 저장된다`() {
            val userInfo = OAuthUserInfo(
                providerId = "google-123456789",
                email = "testuser@gmail.com",
                nickname = null,
            )
            val loginResponse = authService.oauthLogin(JoinProvider.GOOGLE, userInfo)
            val tempToken = requireNotNull(loginResponse.tempToken)

            val signUpBody = signUp(tempToken)
            val tree = objectMapper.readTree(signUpBody)

            assertThat(tree["data"]["accessToken"].asText()).isNotBlank()
            assertThat(tree["data"]["refreshToken"].asText()).isNotBlank()

            val saved = memberRepository.findByProviderIdAndJoinProvider(userInfo.providerId, JoinProvider.GOOGLE)
            assertThat(saved).isNotNull
            assertThat(saved!!.email).isEqualTo(userInfo.email)
            assertThat(saved.joinProvider).isEqualTo(JoinProvider.GOOGLE)
        }

        @Test
        @DisplayName("이미 가입된 providerId로 재가입 시도 - 409 반환")
        fun `중복 회원가입 시도 시 에러를 반환한다`() {
            val userInfo = OAuthUserInfo(providerId = "naver-1", email = null, nickname = "네이버테스터")
            val tempToken = requireNotNull(authService.oauthLogin(JoinProvider.NAVER, userInfo).tempToken)
            signUp(tempToken)

            // tempToken 자체는 1회용이 아니라 JWT 만료 전까지 유효하므로, 같은 tempToken으로 재요청해도
            // DB의 providerId 유니크 제약(existsByProviderIdAndJoinProvider)에서 막혀야 한다
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
    }

    @Test
    @DisplayName("기존 회원이 다시 oauthLogin 하면 LOGIN 상태와 JWT를 반환한다")
    fun `기존 회원은 LOGIN 상태와 JWT를 반환한다`() {
        val userInfo = OAuthUserInfo(providerId = "kakao-existing", email = "a@b.com", nickname = "테스터")
        val tempToken = requireNotNull(authService.oauthLogin(JoinProvider.KAKAO, userInfo).tempToken)
        signUp(tempToken)

        val loginResponse = authService.oauthLogin(JoinProvider.KAKAO, userInfo)

        assertThat(loginResponse.accessToken).isNotBlank()
        assertThat(loginResponse.refreshToken).isNotBlank()
        assertThat(loginResponse.tempToken).isNull()
    }
}
