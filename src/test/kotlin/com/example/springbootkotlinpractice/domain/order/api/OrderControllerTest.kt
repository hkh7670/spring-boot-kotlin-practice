package com.example.springbootkotlinpractice.domain.order.api

import com.example.springbootkotlinpractice.common.security.JwtTokenProvider
import com.example.springbootkotlinpractice.domain.delivery.entity.DeliveryInfo
import com.example.springbootkotlinpractice.domain.delivery.repository.DeliveryInfoRepository
import com.example.springbootkotlinpractice.domain.member.entity.Member
import com.example.springbootkotlinpractice.domain.member.repository.MemberRepository
import com.example.springbootkotlinpractice.domain.order.dto.OrderCreateRequest
import com.example.springbootkotlinpractice.domain.order.dto.OrderItemRequest
import com.example.springbootkotlinpractice.domain.order.repository.OrderDetailInfoRepository
import com.example.springbootkotlinpractice.domain.order.repository.OrderInfoRepository
import com.example.springbootkotlinpractice.domain.product.entity.ProductInfo
import com.example.springbootkotlinpractice.domain.product.repository.ProductInfoRepository
import com.example.springbootkotlinpractice.enums.JoinProvider
import com.fasterxml.jackson.databind.ObjectMapper
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath

private const val BASE_URL = "/api/v1/orders"

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("주문 API 통합 테스트")
class OrderControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var memberRepository: MemberRepository

    @Autowired
    lateinit var productInfoRepository: ProductInfoRepository

    @Autowired
    lateinit var deliveryInfoRepository: DeliveryInfoRepository

    @Autowired
    lateinit var orderInfoRepository: OrderInfoRepository

    @Autowired
    lateinit var orderDetailInfoRepository: OrderDetailInfoRepository

    @Autowired
    lateinit var jwtTokenProvider: JwtTokenProvider

    private lateinit var member: Member
    private lateinit var accessToken: String
    private lateinit var product: ProductInfo
    private lateinit var deliveryInfo: DeliveryInfo

    @BeforeEach
    fun setUp() {
        orderDetailInfoRepository.deleteAll()
        orderInfoRepository.deleteAll()
        productInfoRepository.deleteAll()
        deliveryInfoRepository.deleteAll()
        memberRepository.deleteAll()

        member = memberRepository.save(
            Member.of(
                lastName = "한",
                firstName = "규호",
                birthDate = LocalDate.of(1998, 5, 20),
                phoneNumber = "010-1234-5678",
                email = "orders-test@example.com",
                password = "encoded-password",
                joinProvider = JoinProvider.EMAIL,
            )
        )
        accessToken = jwtTokenProvider.createAccessToken(member.id, member.email, member.joinProvider, member.role)

        product = productInfoRepository.save(ProductInfo(name = "테스트 상품", price = 10_000, stockCount = 5))
        deliveryInfo = deliveryInfoRepository.save(DeliveryInfo(name = "기본 배송", price = 3_000))
    }

    private fun authHeader(token: String = accessToken) = "Bearer $token"

    private fun orderRequestBody(deliveryInfoId: Long, productId: Long, count: Int): String {
        return objectMapper.writeValueAsString(
            OrderCreateRequest(
                deliveryInfoId = deliveryInfoId,
                items = listOf(OrderItemRequest(productId = productId, count = count)),
            )
        )
    }

    @Test
    @DisplayName("정상 주문 생성 시 재고 차감과 주문 상세 저장을 확인한다")
    fun `정상 주문 생성 시 재고 차감과 주문 상세 저장을 확인한다`() {
        mockMvc.post(BASE_URL) {
            header(HttpHeaders.AUTHORIZATION, authHeader())
            contentType = MediaType.APPLICATION_JSON
            content = orderRequestBody(deliveryInfo.id, product.id, 2)
        }.andExpect {
            status { isCreated() }
            jsonPath("$.data.orderUid") { isNotEmpty() }
            jsonPath("$.data.productTotalPrice") { value(20000) }
            jsonPath("$.data.totalPrice") { value(23000) }
        }

        val savedProduct = productInfoRepository.findById(product.id).get()
        assertThat(savedProduct.stockCount).isEqualTo(3)

        val orders = orderInfoRepository.findAll()
        assertThat(orders).hasSize(1)
        val orderDetails = orderDetailInfoRepository.findByOrderId(orders.first().id)
        assertThat(orderDetails).hasSize(1)
        assertThat(orderDetails.first().count).isEqualTo(2)
    }

    @Test
    @DisplayName("재고 부족 시 주문 생성이 실패한다")
    fun `재고 부족 시 주문 생성이 실패한다`() {
        mockMvc.post(BASE_URL) {
            header(HttpHeaders.AUTHORIZATION, authHeader())
            contentType = MediaType.APPLICATION_JSON
            content = orderRequestBody(deliveryInfo.id, product.id, 100)
        }.andExpect {
            status { isConflict() }
        }
    }

    @Test
    @DisplayName("존재하지 않는 상품 주문 시 404를 반환한다")
    fun `존재하지 않는 상품 주문 시 404를 반환한다`() {
        mockMvc.post(BASE_URL) {
            header(HttpHeaders.AUTHORIZATION, authHeader())
            contentType = MediaType.APPLICATION_JSON
            content = orderRequestBody(deliveryInfo.id, 999_999L, 1)
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    @DisplayName("존재하지 않는 배송 정보로 주문 시 404를 반환한다")
    fun `존재하지 않는 배송 정보로 주문 시 404를 반환한다`() {
        mockMvc.post(BASE_URL) {
            header(HttpHeaders.AUTHORIZATION, authHeader())
            contentType = MediaType.APPLICATION_JSON
            content = orderRequestBody(999_999L, product.id, 1)
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    @DisplayName("다른 유저의 주문 조회 시 404를 반환한다")
    fun `다른 유저의 주문 조회 시 404를 반환한다`() {
        val createResult = mockMvc.post(BASE_URL) {
            header(HttpHeaders.AUTHORIZATION, authHeader())
            contentType = MediaType.APPLICATION_JSON
            content = orderRequestBody(deliveryInfo.id, product.id, 1)
        }.andReturn()
        val orderId = objectMapper.readTree(createResult.response.contentAsString)["data"]["orderId"].asLong()

        val otherMember = memberRepository.save(
            Member.of(
                lastName = "김",
                firstName = "철수",
                birthDate = LocalDate.of(1999, 1, 1),
                phoneNumber = "010-9999-0000",
                email = "other-orders-test@example.com",
                password = "encoded-password",
                joinProvider = JoinProvider.EMAIL,
            )
        )
        val otherToken = jwtTokenProvider.createAccessToken(
            otherMember.id, otherMember.email, otherMember.joinProvider, otherMember.role
        )

        mockMvc.get("$BASE_URL/$orderId") {
            header(HttpHeaders.AUTHORIZATION, authHeader(otherToken))
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    @DisplayName("주문 조회 시 결제 전이면 isPaid 가 false 이다")
    fun `주문 조회 시 결제 전이면 isPaid 가 false 이다`() {
        val createResult = mockMvc.post(BASE_URL) {
            header(HttpHeaders.AUTHORIZATION, authHeader())
            contentType = MediaType.APPLICATION_JSON
            content = orderRequestBody(deliveryInfo.id, product.id, 1)
        }.andReturn()
        val orderId = objectMapper.readTree(createResult.response.contentAsString)["data"]["orderId"].asLong()

        mockMvc.get("$BASE_URL/$orderId") {
            header(HttpHeaders.AUTHORIZATION, authHeader())
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.isPaid") { value(false) }
            jsonPath("$.data.items.length()") { value(1) }
        }
    }
}
