package com.example.springbootkotlinpractice.domain.point.api

import com.example.springbootkotlinpractice.common.security.JwtTokenProvider
import com.example.springbootkotlinpractice.domain.member.entity.Member
import com.example.springbootkotlinpractice.domain.member.repository.MemberRepository
import com.example.springbootkotlinpractice.domain.point.dto.AdminPointRequest
import com.example.springbootkotlinpractice.domain.point.dto.PointGrantRequest
import com.example.springbootkotlinpractice.domain.point.entity.Point
import com.example.springbootkotlinpractice.domain.point.repository.MemberPointRepository
import com.example.springbootkotlinpractice.domain.point.repository.PointRepository
import com.example.springbootkotlinpractice.enums.JoinProvider
import com.example.springbootkotlinpractice.enums.MemberPointStatus
import com.example.springbootkotlinpractice.enums.Role
import com.fasterxml.jackson.databind.ObjectMapper
import java.time.LocalDate
import java.time.LocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post

private const val BASE_URL = "/api/v1/admin/points"

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("관리자 포인트 API 통합 테스트")
class AdminPointControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var pointRepository: PointRepository

    @Autowired
    lateinit var memberPointRepository: MemberPointRepository

    @Autowired
    lateinit var memberRepository: MemberRepository

    @Autowired
    lateinit var jwtTokenProvider: JwtTokenProvider

    private lateinit var adminToken: String
    private lateinit var memberA: Member
    private lateinit var memberB: Member

    @BeforeEach
    fun setUp() {
        memberPointRepository.deleteAll()
        pointRepository.deleteAll()
        memberRepository.deleteAll()

        adminToken = jwtTokenProvider.createAdminAccessToken(1L)
        memberA = saveMember("point-test-a@example.com")
        memberB = saveMember("point-test-b@example.com")
    }

    private fun saveMember(email: String): Member {
        return memberRepository.save(
            Member.of(
                lastName = "포인트",
                firstName = "테스트",
                birthDate = LocalDate.of(1998, 5, 20),
                phoneNumber = "010-1234-5678",
                email = email,
                password = "encoded-password",
                joinProvider = JoinProvider.EMAIL,
            )
        )
    }

    private fun bearer(token: String) = "Bearer $token"

    private fun memberToken(member: Member): String {
        return jwtTokenProvider.createAccessToken(
            member.id, member.email, member.joinProvider, Role.USER
        )
    }

    private fun savePoint(validDays: Int? = 365): Point {
        return pointRepository.save(Point.of(name = "테스트 포인트", validDays = validDays))
    }

    private fun pointBody(name: String = "이벤트 지급", validDays: Int? = 30): String {
        return objectMapper.writeValueAsString(AdminPointRequest(name = name, validDays = validDays))
    }

    private fun grant(pointId: Long, amount: Int, vararg memberIds: Long) =
        mockMvc.post("$BASE_URL/$pointId/grant") {
            header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                PointGrantRequest(memberIds = memberIds.toList(), amount = amount)
            )
        }

    @Test
    @DisplayName("토큰 없이 포인트를 생성하면 401, 일반 회원 토큰이면 403을 반환한다")
    fun `토큰 없이 포인트를 생성하면 401 일반 회원 토큰이면 403을 반환한다`() {
        mockMvc.post(BASE_URL) {
            contentType = MediaType.APPLICATION_JSON
            content = pointBody()
        }.andExpect { status { isUnauthorized() } }

        mockMvc.post(BASE_URL) {
            header(HttpHeaders.AUTHORIZATION, bearer(memberToken(memberA)))
            contentType = MediaType.APPLICATION_JSON
            content = pointBody()
        }.andExpect { status { isForbidden() } }

        assertThat(pointRepository.count()).isZero()
    }

    @Test
    @DisplayName("포인트 템플릿을 생성한다 (유효 일수 없이도 생성할 수 있다)")
    fun `포인트 템플릿을 생성한다`() {
        mockMvc.post(BASE_URL) {
            header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
            contentType = MediaType.APPLICATION_JSON
            content = pointBody(validDays = null)
        }.andExpect {
            status { isCreated() }
            jsonPath("$.data.name") { value("이벤트 지급") }
            jsonPath("$.data.validDays") { doesNotExist() }
        }

        assertThat(pointRepository.findAll().single().isDeleted).isFalse()
    }

    @Test
    @DisplayName("이름이 비어 있거나 유효 일수가 0 이하면 400을 반환한다")
    fun `이름이 비어 있거나 유효 일수가 0 이하면 400을 반환한다`() {
        mockMvc.post(BASE_URL) {
            header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
            contentType = MediaType.APPLICATION_JSON
            content = pointBody(name = " ")
        }.andExpect { status { isBadRequest() } }

        mockMvc.post(BASE_URL) {
            header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
            contentType = MediaType.APPLICATION_JSON
            content = pointBody(validDays = 0)
        }.andExpect { status { isBadRequest() } }

        assertThat(pointRepository.count()).isZero()
    }

    @Test
    @DisplayName("포인트 목록은 삭제된 템플릿을 제외하고 단건 조회는 삭제된 템플릿에 404를 반환한다")
    fun `포인트 목록은 삭제된 템플릿을 제외하고 단건 조회는 삭제된 템플릿에 404를 반환한다`() {
        val kept = savePoint()
        val removed = savePoint()
        removed.delete()
        pointRepository.save(removed)

        mockMvc.get(BASE_URL) {
            header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.totalElements") { value(1) }
            jsonPath("$.data.content[0].id") { value(kept.id) }
        }
        mockMvc.get("$BASE_URL/${kept.id}") {
            header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
        }.andExpect { status { isOk() } }
        mockMvc.get("$BASE_URL/${removed.id}") {
            header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
        }.andExpect { status { isNotFound() } }
    }

    @Test
    @DisplayName("포인트 템플릿을 수정한다")
    fun `포인트 템플릿을 수정한다`() {
        val point = savePoint(validDays = 365)

        mockMvc.patch("$BASE_URL/${point.id}") {
            header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
            contentType = MediaType.APPLICATION_JSON
            content = pointBody(name = "수정된 포인트", validDays = 90)
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.name") { value("수정된 포인트") }
            jsonPath("$.data.validDays") { value(90) }
        }

        val saved = pointRepository.findById(point.id).get()
        assertThat(saved.name).isEqualTo("수정된 포인트")
        assertThat(saved.validDays).isEqualTo(90)
    }

    @Test
    @DisplayName("존재하지 않는 포인트를 수정하면 404를 반환한다")
    fun `존재하지 않는 포인트를 수정하면 404를 반환한다`() {
        mockMvc.patch("$BASE_URL/999999") {
            header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
            contentType = MediaType.APPLICATION_JSON
            content = pointBody()
        }.andExpect { status { isNotFound() } }
    }

    @Test
    @DisplayName("포인트를 회원들에게 적립하면 잔액이 생기고 만료 일시가 유효 일수만큼 뒤로 설정된다")
    fun `포인트를 회원들에게 적립하면 잔액이 생기고 만료 일시가 유효 일수만큼 뒤로 설정된다`() {
        val point = savePoint(validDays = 365)
        val before = LocalDateTime.now()

        grant(point.id, 5_000, memberA.id, memberB.id).andExpect {
            status { isCreated() }
            jsonPath("$.data.grantedCount") { value(2) }
        }

        val granted = memberPointRepository.findAll()
        assertThat(granted.map { it.memberId }).containsExactlyInAnyOrder(memberA.id, memberB.id)
        assertThat(granted).allSatisfy {
            assertThat(it.pointId).isEqualTo(point.id)
            assertThat(it.amount).isEqualTo(5_000)
            assertThat(it.remainingAmount).isEqualTo(5_000)
            assertThat(it.status).isEqualTo(MemberPointStatus.ACTIVE)
            assertThat(it.expiredAt).isAfter(before.plusDays(364)).isBefore(before.plusDays(366))
        }
        mockMvc.get("/api/v1/points") {
            header(HttpHeaders.AUTHORIZATION, bearer(memberToken(memberA)))
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.usableAmount") { value(5000) }
        }
    }

    @Test
    @DisplayName("유효 일수가 없는 포인트는 만료 없이 적립된다")
    fun `유효 일수가 없는 포인트는 만료 없이 적립된다`() {
        val point = savePoint(validDays = null)

        grant(point.id, 1_000, memberA.id).andExpect { status { isCreated() } }

        assertThat(memberPointRepository.findAll().single().expiredAt).isNull()
    }

    @Test
    @DisplayName("같은 포인트를 같은 회원에게 여러 번 적립할 수 있다")
    fun `같은 포인트를 같은 회원에게 여러 번 적립할 수 있다`() {
        val point = savePoint()

        grant(point.id, 1_000, memberA.id).andExpect { status { isCreated() } }
        grant(point.id, 2_000, memberA.id).andExpect { status { isCreated() } }

        mockMvc.get("/api/v1/points") {
            header(HttpHeaders.AUTHORIZATION, bearer(memberToken(memberA)))
        }.andExpect { jsonPath("$.data.usableAmount") { value(3000) } }
    }

    @Test
    @DisplayName("존재하지 않는 회원이 포함되면 404를 반환하고 아무에게도 적립하지 않는다")
    fun `존재하지 않는 회원이 포함되면 404를 반환하고 아무에게도 적립하지 않는다`() {
        val point = savePoint()

        grant(point.id, 1_000, memberA.id, 999_999L).andExpect { status { isNotFound() } }

        assertThat(memberPointRepository.count()).isZero()
    }

    @Test
    @DisplayName("적립 금액이 0 이하이거나 대상 회원이 비어 있으면 400을 반환한다")
    fun `적립 금액이 0 이하이거나 대상 회원이 비어 있으면 400을 반환한다`() {
        val point = savePoint()

        grant(point.id, 0, memberA.id).andExpect { status { isBadRequest() } }
        grant(point.id, -100, memberA.id).andExpect { status { isBadRequest() } }
        grant(point.id, 1_000).andExpect { status { isBadRequest() } }

        assertThat(memberPointRepository.count()).isZero()
    }

    @Test
    @DisplayName("적립 금액이나 유효 일수가 상한을 넘으면 400을 반환한다")
    fun `적립 금액이나 유효 일수가 상한을 넘으면 400을 반환한다`() {
        val point = savePoint()

        grant(point.id, 10_000_001, memberA.id).andExpect { status { isBadRequest() } }
        mockMvc.post(BASE_URL) {
            header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
            contentType = MediaType.APPLICATION_JSON
            content = pointBody(validDays = 3651)
        }.andExpect { status { isBadRequest() } }

        assertThat(memberPointRepository.count()).isZero()
    }

    @Test
    @DisplayName("포인트 목록의 페이지 번호나 크기가 범위를 벗어나면 400을 반환한다")
    fun `포인트 목록의 페이지 번호나 크기가 범위를 벗어나면 400을 반환한다`() {
        listOf("size=0", "size=101", "page=-1").forEach { query ->
            mockMvc.get("$BASE_URL?$query") {
                header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
            }.andExpect { status { isBadRequest() } }
        }
    }

    @Test
    @DisplayName("포인트 템플릿을 삭제해도 이미 적립된 포인트는 계속 사용할 수 있고 신규 적립은 막힌다")
    fun `포인트 템플릿을 삭제해도 이미 적립된 포인트는 계속 사용할 수 있고 신규 적립은 막힌다`() {
        val point = savePoint()
        grant(point.id, 5_000, memberA.id).andExpect { status { isCreated() } }

        mockMvc.delete("$BASE_URL/${point.id}") {
            header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
        }.andExpect { status { isOk() } }

        assertThat(pointRepository.findById(point.id).get().isDeleted).isTrue()
        mockMvc.get("/api/v1/points") {
            header(HttpHeaders.AUTHORIZATION, bearer(memberToken(memberA)))
        }.andExpect { jsonPath("$.data.usableAmount") { value(5000) } }
        grant(point.id, 1_000, memberB.id).andExpect { status { isNotFound() } }
    }
}
