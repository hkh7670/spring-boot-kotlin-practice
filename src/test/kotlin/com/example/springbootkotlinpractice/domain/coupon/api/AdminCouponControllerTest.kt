package com.example.springbootkotlinpractice.domain.coupon.api

import com.example.springbootkotlinpractice.common.security.JwtTokenProvider
import com.example.springbootkotlinpractice.domain.coupon.dto.AdminCouponCreateRequest
import com.example.springbootkotlinpractice.domain.coupon.dto.AdminCouponUpdateRequest
import com.example.springbootkotlinpractice.domain.coupon.dto.CouponIssueRequest
import com.example.springbootkotlinpractice.domain.coupon.entity.Coupon
import com.example.springbootkotlinpractice.domain.coupon.repository.CouponRepository
import com.example.springbootkotlinpractice.domain.coupon.repository.MemberCouponRepository
import com.example.springbootkotlinpractice.domain.member.entity.Member
import com.example.springbootkotlinpractice.domain.member.repository.MemberRepository
import com.example.springbootkotlinpractice.enums.CouponDiscountType
import com.example.springbootkotlinpractice.enums.JoinProvider
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

private const val BASE_URL = "/api/v1/admin/coupons"

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("관리자 쿠폰 API 통합 테스트")
class AdminCouponControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var couponRepository: CouponRepository

    @Autowired
    lateinit var memberCouponRepository: MemberCouponRepository

    @Autowired
    lateinit var memberRepository: MemberRepository

    @Autowired
    lateinit var jwtTokenProvider: JwtTokenProvider

    private lateinit var adminToken: String
    private lateinit var memberA: Member
    private lateinit var memberB: Member

    @BeforeEach
    fun setUp() {
        memberCouponRepository.deleteAll()
        couponRepository.deleteAll()
        memberRepository.deleteAll()

        adminToken = jwtTokenProvider.createAdminAccessToken(1L)
        memberA = saveMember("coupon-test-a@example.com")
        memberB = saveMember("coupon-test-b@example.com")
    }

    private fun saveMember(email: String): Member {
        return memberRepository.save(
            Member.of(
                lastName = "쿠폰",
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

    private fun saveCoupon(
        validUntil: LocalDateTime = LocalDateTime.now().plusDays(30),
    ): Coupon {
        return couponRepository.save(
            Coupon.of(
                name = "테스트 쿠폰",
                discountType = CouponDiscountType.FIXED,
                discountValue = 5_000,
                minOrderPrice = 30_000,
                validUntil = validUntil,
            )
        )
    }

    private fun createBody(
        discountType: CouponDiscountType = CouponDiscountType.FIXED,
        discountValue: Int = 5_000,
        maxDiscountPrice: Int? = null,
        validUntil: LocalDateTime = LocalDateTime.now().plusDays(30),
    ): String {
        return objectMapper.writeValueAsString(
            AdminCouponCreateRequest(
                name = "신규 쿠폰",
                discountType = discountType,
                discountValue = discountValue,
                maxDiscountPrice = maxDiscountPrice,
                minOrderPrice = 10_000,
                validUntil = validUntil,
            )
        )
    }

    private fun updateBody(name: String = "수정 쿠폰", discountValue: Int = 7_000): String {
        return objectMapper.writeValueAsString(
            AdminCouponUpdateRequest(
                name = name,
                discountValue = discountValue,
                maxDiscountPrice = null,
                minOrderPrice = 30_000,
            )
        )
    }

    private fun issueBody(vararg memberIds: Long): String {
        return objectMapper.writeValueAsString(CouponIssueRequest(memberIds = memberIds.toList()))
    }

    private fun issue(couponId: Long, vararg memberIds: Long) = mockMvc.post("$BASE_URL/$couponId/issue") {
        header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
        contentType = MediaType.APPLICATION_JSON
        content = issueBody(*memberIds)
    }

    @Test
    @DisplayName("토큰 없이 쿠폰을 생성하면 401, 일반 회원 토큰이면 403을 반환한다")
    fun `토큰 없이 쿠폰을 생성하면 401 일반 회원 토큰이면 403을 반환한다`() {
        mockMvc.post(BASE_URL) {
            contentType = MediaType.APPLICATION_JSON
            content = createBody()
        }.andExpect { status { isUnauthorized() } }

        mockMvc.post(BASE_URL) {
            header(HttpHeaders.AUTHORIZATION, bearer(memberToken(memberA)))
            contentType = MediaType.APPLICATION_JSON
            content = createBody()
        }.andExpect { status { isForbidden() } }

        assertThat(couponRepository.count()).isZero()
    }

    @Test
    @DisplayName("정액 쿠폰 템플릿을 생성한다")
    fun `정액 쿠폰 템플릿을 생성한다`() {
        mockMvc.post(BASE_URL) {
            header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
            contentType = MediaType.APPLICATION_JSON
            content = createBody()
        }.andExpect {
            status { isCreated() }
            jsonPath("$.data.name") { value("신규 쿠폰") }
            jsonPath("$.data.discountType") { value("FIXED") }
            jsonPath("$.data.discountValue") { value(5000) }
        }

        val saved = couponRepository.findAll().single()
        assertThat(saved.isDeleted).isFalse()
    }

    @Test
    @DisplayName("정률 쿠폰의 할인율이 100을 넘으면 400을 반환한다")
    fun `정률 쿠폰의 할인율이 100을 넘으면 400을 반환한다`() {
        mockMvc.post(BASE_URL) {
            header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
            contentType = MediaType.APPLICATION_JSON
            content = createBody(discountType = CouponDiscountType.PERCENTAGE, discountValue = 150)
        }.andExpect { status { isBadRequest() } }

        assertThat(couponRepository.count()).isZero()
    }

    @Test
    @DisplayName("정액 쿠폰에 최대 할인 금액을 지정하면 400을 반환한다")
    fun `정액 쿠폰에 최대 할인 금액을 지정하면 400을 반환한다`() {
        mockMvc.post(BASE_URL) {
            header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
            contentType = MediaType.APPLICATION_JSON
            content = createBody(maxDiscountPrice = 3_000)
        }.andExpect { status { isBadRequest() } }
    }

    @Test
    @DisplayName("마감 일시가 과거인 쿠폰은 생성할 수 없다")
    fun `마감 일시가 과거인 쿠폰은 생성할 수 없다`() {
        mockMvc.post(BASE_URL) {
            header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
            contentType = MediaType.APPLICATION_JSON
            content = createBody(validUntil = LocalDateTime.now().minusDays(1))
        }.andExpect { status { isBadRequest() } }
    }

    @Test
    @DisplayName("쿠폰 목록은 삭제된 쿠폰을 제외하고 단건 조회는 삭제된 쿠폰에 404를 반환한다")
    fun `쿠폰 목록은 삭제된 쿠폰을 제외하고 단건 조회는 삭제된 쿠폰에 404를 반환한다`() {
        val kept = saveCoupon()
        val removed = saveCoupon()
        removed.delete()
        couponRepository.save(removed)

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
    @DisplayName("발급 이력이 없는 쿠폰은 할인 조건까지 수정할 수 있다")
    fun `발급 이력이 없는 쿠폰은 할인 조건까지 수정할 수 있다`() {
        val coupon = saveCoupon()

        mockMvc.patch("$BASE_URL/${coupon.id}") {
            header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
            contentType = MediaType.APPLICATION_JSON
            content = updateBody()
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.discountValue") { value(7000) }
        }

        val saved = couponRepository.findById(coupon.id).get()
        assertThat(saved.name).isEqualTo("수정 쿠폰")
        assertThat(saved.discountValue).isEqualTo(7_000)
    }

    @Test
    @DisplayName("발급 이력이 있는 쿠폰은 할인 조건 수정이 409로 거부되고 이름만 수정할 수 있다")
    fun `발급 이력이 있는 쿠폰은 할인 조건 수정이 409로 거부되고 이름만 수정할 수 있다`() {
        val coupon = saveCoupon()
        issue(coupon.id, memberA.id).andExpect { status { isCreated() } }

        mockMvc.patch("$BASE_URL/${coupon.id}") {
            header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
            contentType = MediaType.APPLICATION_JSON
            content = updateBody(discountValue = 7_000)
        }.andExpect { status { isConflict() } }
        assertThat(couponRepository.findById(coupon.id).get().discountValue).isEqualTo(5_000)

        mockMvc.patch("$BASE_URL/${coupon.id}") {
            header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
            contentType = MediaType.APPLICATION_JSON
            content = updateBody(discountValue = 5_000)
        }.andExpect { status { isOk() } }
        assertThat(couponRepository.findById(coupon.id).get().name).isEqualTo("수정 쿠폰")
    }

    @Test
    @DisplayName("쿠폰을 삭제해도 이미 발급된 쿠폰은 회원이 계속 조회할 수 있다")
    fun `쿠폰을 삭제해도 이미 발급된 쿠폰은 회원이 계속 조회할 수 있다`() {
        val coupon = saveCoupon()
        issue(coupon.id, memberA.id).andExpect { status { isCreated() } }

        mockMvc.delete("$BASE_URL/${coupon.id}") {
            header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
        }.andExpect { status { isOk() } }

        assertThat(couponRepository.findById(coupon.id).get().isDeleted).isTrue()
        mockMvc.get("/api/v1/coupons") {
            header(HttpHeaders.AUTHORIZATION, bearer(memberToken(memberA)))
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.length()") { value(1) }
            jsonPath("$.data[0].couponName") { value("테스트 쿠폰") }
        }
        issue(coupon.id, memberB.id).andExpect { status { isNotFound() } }
    }

    @Test
    @DisplayName("쿠폰을 회원들에게 발급하면 만료 일시가 템플릿 마감 일시로 고정된다")
    fun `쿠폰을 회원들에게 발급하면 만료 일시가 템플릿 마감 일시로 고정된다`() {
        val coupon = saveCoupon()

        issue(coupon.id, memberA.id, memberB.id).andExpect {
            status { isCreated() }
            jsonPath("$.data.issuedCount") { value(2) }
        }

        val issued = memberCouponRepository.findAll()
        assertThat(issued.map { it.memberId }).containsExactlyInAnyOrder(memberA.id, memberB.id)
        assertThat(issued).allSatisfy {
            assertThat(it.couponId).isEqualTo(coupon.id)
            assertThat(it.expiredAt).isEqualTo(coupon.validUntil)
        }
    }

    @Test
    @DisplayName("이미 발급받은 회원이 포함되면 409를 반환하고 아무에게도 발급하지 않는다")
    fun `이미 발급받은 회원이 포함되면 409를 반환하고 아무에게도 발급하지 않는다`() {
        val coupon = saveCoupon()
        issue(coupon.id, memberA.id).andExpect { status { isCreated() } }

        issue(coupon.id, memberA.id, memberB.id).andExpect { status { isConflict() } }

        assertThat(memberCouponRepository.findAll().map { it.memberId }).containsExactly(memberA.id)
    }

    @Test
    @DisplayName("존재하지 않는 회원이 포함되면 404를 반환하고 아무에게도 발급하지 않는다")
    fun `존재하지 않는 회원이 포함되면 404를 반환하고 아무에게도 발급하지 않는다`() {
        val coupon = saveCoupon()

        issue(coupon.id, memberA.id, 999_999L).andExpect { status { isNotFound() } }

        assertThat(memberCouponRepository.count()).isZero()
    }

    @Test
    @DisplayName("마감된 쿠폰은 발급할 수 없다")
    fun `마감된 쿠폰은 발급할 수 없다`() {
        val expired = saveCoupon(validUntil = LocalDateTime.now().minusDays(1))

        issue(expired.id, memberA.id).andExpect { status { isBadRequest() } }

        assertThat(memberCouponRepository.count()).isZero()
    }

    @Test
    @DisplayName("할인값이나 최소 주문 금액이 상한을 넘으면 400을 반환한다")
    fun `할인값이나 최소 주문 금액이 상한을 넘으면 400을 반환한다`() {
        mockMvc.post(BASE_URL) {
            header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
            contentType = MediaType.APPLICATION_JSON
            content = createBody(discountValue = 10_000_001)
        }.andExpect { status { isBadRequest() } }

        assertThat(couponRepository.count()).isZero()
    }

    @Test
    @DisplayName("쿠폰 목록의 페이지 번호나 크기가 범위를 벗어나면 400을 반환한다")
    fun `쿠폰 목록의 페이지 번호나 크기가 범위를 벗어나면 400을 반환한다`() {
        listOf("size=0", "size=101", "page=-1").forEach { query ->
            mockMvc.get("$BASE_URL?$query") {
                header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
            }.andExpect { status { isBadRequest() } }
        }
    }

    @Test
    @DisplayName("발급 대상 회원이 비어 있으면 400을 반환한다")
    fun `발급 대상 회원이 비어 있으면 400을 반환한다`() {
        val coupon = saveCoupon()

        issue(coupon.id).andExpect { status { isBadRequest() } }
    }
}
