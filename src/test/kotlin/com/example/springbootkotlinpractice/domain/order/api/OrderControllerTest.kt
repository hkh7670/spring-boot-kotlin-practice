package com.example.springbootkotlinpractice.domain.order.api

import com.example.springbootkotlinpractice.common.payment.toss.TossCancelPaymentRequest
import com.example.springbootkotlinpractice.common.payment.toss.TossCancelPaymentResponse
import com.example.springbootkotlinpractice.common.payment.toss.TossPaymentsApi
import com.example.springbootkotlinpractice.common.security.JwtTokenProvider
import com.example.springbootkotlinpractice.domain.delivery.entity.DeliveryOption
import com.example.springbootkotlinpractice.domain.delivery.repository.DeliveryOptionRepository
import com.example.springbootkotlinpractice.domain.member.entity.Member
import com.example.springbootkotlinpractice.domain.member.repository.MemberRepository
import com.example.springbootkotlinpractice.domain.order.dto.OrderCreateRequest
import com.example.springbootkotlinpractice.domain.order.dto.OrderItemRequest
import com.example.springbootkotlinpractice.domain.order.entity.Order
import com.example.springbootkotlinpractice.domain.order.entity.OrderItem
import com.example.springbootkotlinpractice.domain.order.repository.OrderItemRepository
import com.example.springbootkotlinpractice.domain.order.repository.OrderRepository
import com.example.springbootkotlinpractice.domain.payment.entity.Payment
import com.example.springbootkotlinpractice.domain.payment.repository.PaymentRepository
import com.example.springbootkotlinpractice.domain.product.entity.Product
import com.example.springbootkotlinpractice.domain.product.entity.ProductOption
import com.example.springbootkotlinpractice.domain.product.repository.ProductOptionRepository
import com.example.springbootkotlinpractice.domain.product.repository.ProductRepository
import com.example.springbootkotlinpractice.enums.JoinProvider
import com.example.springbootkotlinpractice.enums.OrderStatus
import com.example.springbootkotlinpractice.enums.PaymentStatus
import com.example.springbootkotlinpractice.enums.ResponseCodeEnum
import com.example.springbootkotlinpractice.exception.ApiErrorException
import com.fasterxml.jackson.databind.ObjectMapper
import java.time.LocalDate
import java.time.LocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

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
    lateinit var productRepository: ProductRepository

    @Autowired
    lateinit var productOptionRepository: ProductOptionRepository

    @Autowired
    lateinit var deliveryOptionRepository: DeliveryOptionRepository

    @Autowired
    lateinit var orderRepository: OrderRepository

    @Autowired
    lateinit var orderItemRepository: OrderItemRepository

    @Autowired
    lateinit var paymentRepository: PaymentRepository

    @Autowired
    lateinit var jwtTokenProvider: JwtTokenProvider

    @MockitoBean
    lateinit var tossPaymentsApi: TossPaymentsApi

    private lateinit var member: Member
    private lateinit var accessToken: String
    private lateinit var product: Product
    private lateinit var productOption: ProductOption
    private lateinit var deliveryOption: DeliveryOption

    @BeforeEach
    fun setUp() {
        orderItemRepository.deleteAll()
        paymentRepository.deleteAll()
        orderRepository.deleteAll()
        productOptionRepository.deleteAll()
        productRepository.deleteAll()
        deliveryOptionRepository.deleteAll()
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

        product = productRepository.save(Product(name = "테스트 상품"))
        productOption = productOptionRepository.save(
            ProductOption.of(product = product, name = "기본", price = 10_000, stockCount = 5)
        )
        deliveryOption = deliveryOptionRepository.save(DeliveryOption(name = "기본 배송", price = 3_000))
    }

    private fun authHeader(token: String = accessToken) = "Bearer $token"

    private fun orderRequestBody(deliveryOptionId: Long, productOptionId: Long, count: Int): String {
        return objectMapper.writeValueAsString(
            OrderCreateRequest(
                deliveryOptionId = deliveryOptionId,
                items = listOf(OrderItemRequest(productOptionId = productOptionId, count = count)),
            )
        )
    }

    @Test
    @DisplayName("정상 주문 생성 시 재고 차감과 주문 상세 저장을 확인한다")
    fun `정상 주문 생성 시 재고 차감과 주문 상세 저장을 확인한다`() {
        mockMvc.post(BASE_URL) {
            header(HttpHeaders.AUTHORIZATION, authHeader())
            contentType = MediaType.APPLICATION_JSON
            content = orderRequestBody(deliveryOption.id, productOption.id, 2)
        }.andExpect {
            status { isCreated() }
            jsonPath("$.data.orderUid") { isNotEmpty() }
            jsonPath("$.data.productTotalPrice") { value(20000) }
            jsonPath("$.data.totalPrice") { value(23000) }
        }

        val savedProductOption = productOptionRepository.findById(productOption.id).get()
        assertThat(savedProductOption.stockCount).isEqualTo(3)

        val orders = orderRepository.findAll()
        assertThat(orders).hasSize(1)
        val orderItems = orderItemRepository.findByOrderId(orders.first().id)
        assertThat(orderItems).hasSize(1)
        assertThat(orderItems.first().count).isEqualTo(2)
    }

    @Test
    @DisplayName("재고 부족 시 주문 생성이 실패한다")
    fun `재고 부족 시 주문 생성이 실패한다`() {
        mockMvc.post(BASE_URL) {
            header(HttpHeaders.AUTHORIZATION, authHeader())
            contentType = MediaType.APPLICATION_JSON
            content = orderRequestBody(deliveryOption.id, productOption.id, 100)
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
            content = orderRequestBody(deliveryOption.id, 999_999L, 1)
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
            content = orderRequestBody(999_999L, productOption.id, 1)
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
            content = orderRequestBody(deliveryOption.id, productOption.id, 1)
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
            content = orderRequestBody(deliveryOption.id, productOption.id, 1)
        }.andReturn()
        val orderId = objectMapper.readTree(createResult.response.contentAsString)["data"]["orderId"].asLong()

        mockMvc.get("$BASE_URL/$orderId") {
            header(HttpHeaders.AUTHORIZATION, authHeader())
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.isPaid") { value(false) }
            jsonPath("$.data.itemList.length()") { value(1) }
        }
    }

    private fun createPaidOrder(count: Int = 1): Order {
        productOption.stockCount -= count
        productOptionRepository.save(productOption)

        val order = orderRepository.save(
            Order.of(
                memberId = member.id,
                productTotalPrice = productOption.price * count,
                deliveryOptionId = deliveryOption.id,
                deliveryPrice = deliveryOption.price,
            )
        )
        orderItemRepository.save(
            OrderItem.of(order = order, productOption = productOption, price = productOption.price.toLong(), count = count)
        )
        order.markPaid()
        orderRepository.save(order)

        paymentRepository.save(
            Payment.of(
                orderId = order.id,
                paymentKey = FAKE_PAYMENT_KEY,
                amount = productOption.price * count,
                status = PaymentStatus.DONE,
                method = "카드",
                approvedAt = LocalDateTime.now(),
            )
        )

        return order
    }

    private fun stubTossCancelSuccess(paymentKey: String = FAKE_PAYMENT_KEY) {
        given(tossPaymentsApi.cancelPayment(paymentKey, TossCancelPaymentRequest(cancelReason = CANCEL_REASON)))
            .willReturn(TossCancelPaymentResponse(paymentKey = paymentKey, orderId = "toss-order-id", status = "CANCELED"))
    }

    private fun stubTossCancelFailure(paymentKey: String = FAKE_PAYMENT_KEY) {
        given(tossPaymentsApi.cancelPayment(paymentKey, TossCancelPaymentRequest(cancelReason = CANCEL_REASON)))
            .willThrow(ApiErrorException(ResponseCodeEnum.PAYMENT_CANCEL_FAILED))
    }

    @Test
    @DisplayName("결제 완료된 주문 취소 시 Toss 취소가 성공하면 주문/결제가 취소 상태가 되고 재고가 복구된다")
    fun `결제 완료된 주문 취소 시 Toss 취소가 성공하면 주문이 취소된다`() {
        val order = createPaidOrder(count = 2)
        stubTossCancelSuccess()

        mockMvc.post("$BASE_URL/${order.id}/cancel") {
            header(HttpHeaders.AUTHORIZATION, authHeader())
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.status") { value(OrderStatus.CANCELLED.name) }
        }

        assertThat(orderRepository.findById(order.id).get().status).isEqualTo(OrderStatus.CANCELLED)
        assertThat(paymentRepository.findByOrderId(order.id)!!.status).isEqualTo(PaymentStatus.CANCELED)
        assertThat(productOptionRepository.findById(productOption.id).get().stockCount).isEqualTo(5)
    }

    @Test
    @DisplayName("Toss 취소가 실패하면 DB에는 아무 변경도 반영되지 않는다")
    fun `Toss 취소 실패 시 주문 결제 재고 상태가 그대로 유지된다`() {
        val order = createPaidOrder(count = 2)
        stubTossCancelFailure()

        val result = mockMvc.post("$BASE_URL/${order.id}/cancel") {
            header(HttpHeaders.AUTHORIZATION, authHeader())
        }.andReturn()

        assertThat(result.response.status).isEqualTo(502)
        assertThat(orderRepository.findById(order.id).get().status).isEqualTo(OrderStatus.PAID)
        assertThat(paymentRepository.findByOrderId(order.id)!!.status).isEqualTo(PaymentStatus.DONE)
        assertThat(productOptionRepository.findById(productOption.id).get().stockCount).isEqualTo(3)
    }

    @Test
    @DisplayName("이미 취소된 주문을 다시 취소하면 409를 반환한다")
    fun `이미 취소된 주문을 다시 취소하면 409를 반환한다`() {
        val order = createPaidOrder()
        stubTossCancelSuccess()

        mockMvc.post("$BASE_URL/${order.id}/cancel") {
            header(HttpHeaders.AUTHORIZATION, authHeader())
        }.andExpect { status { isOk() } }

        mockMvc.post("$BASE_URL/${order.id}/cancel") {
            header(HttpHeaders.AUTHORIZATION, authHeader())
        }.andExpect {
            status { isConflict() }
        }
    }

    @Test
    @DisplayName("결제 전 주문을 취소하면 409를 반환한다")
    fun `결제 전 주문을 취소하면 409를 반환한다`() {
        val createResult = mockMvc.post(BASE_URL) {
            header(HttpHeaders.AUTHORIZATION, authHeader())
            contentType = MediaType.APPLICATION_JSON
            content = orderRequestBody(deliveryOption.id, productOption.id, 1)
        }.andReturn()
        val orderId = objectMapper.readTree(createResult.response.contentAsString)["data"]["orderId"].asLong()

        mockMvc.post("$BASE_URL/$orderId/cancel") {
            header(HttpHeaders.AUTHORIZATION, authHeader())
        }.andExpect {
            status { isConflict() }
        }
    }

    @Test
    @DisplayName("다른 유저의 주문을 취소하면 404를 반환한다")
    fun `다른 유저의 주문을 취소하면 404를 반환한다`() {
        val order = createPaidOrder()

        val otherMember = memberRepository.save(
            Member.of(
                lastName = "김",
                firstName = "철수",
                birthDate = LocalDate.of(1999, 1, 1),
                phoneNumber = "010-9999-0000",
                email = "other-cancel-test@example.com",
                password = "encoded-password",
                joinProvider = JoinProvider.EMAIL,
            )
        )
        val otherToken = jwtTokenProvider.createAccessToken(
            otherMember.id, otherMember.email, otherMember.joinProvider, otherMember.role
        )

        mockMvc.post("$BASE_URL/${order.id}/cancel") {
            header(HttpHeaders.AUTHORIZATION, authHeader(otherToken))
        }.andExpect {
            status { isNotFound() }
        }
    }

    companion object {
        private const val FAKE_PAYMENT_KEY = "fake-payment-key"
        private const val CANCEL_REASON = "고객 요청에 의한 주문 취소"
    }
}
