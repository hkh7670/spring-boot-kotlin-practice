package com.example.springbootkotlinpractice.domain.payment.api

import com.example.springbootkotlinpractice.common.payment.toss.TossConfirmPaymentRequest
import com.example.springbootkotlinpractice.common.payment.toss.TossConfirmPaymentResponse
import com.example.springbootkotlinpractice.common.payment.toss.TossPaymentsApi
import com.example.springbootkotlinpractice.common.security.JwtTokenProvider
import com.example.springbootkotlinpractice.domain.delivery.entity.DeliveryInfo
import com.example.springbootkotlinpractice.domain.delivery.repository.DeliveryInfoRepository
import com.example.springbootkotlinpractice.domain.member.entity.Member
import com.example.springbootkotlinpractice.domain.member.repository.MemberRepository
import com.example.springbootkotlinpractice.domain.order.entity.OrderDetailInfo
import com.example.springbootkotlinpractice.domain.order.entity.OrderInfo
import com.example.springbootkotlinpractice.domain.order.repository.OrderDetailInfoRepository
import com.example.springbootkotlinpractice.domain.order.repository.OrderInfoRepository
import com.example.springbootkotlinpractice.domain.payment.dto.PaymentConfirmRequest
import com.example.springbootkotlinpractice.domain.payment.repository.PayInfoRepository
import com.example.springbootkotlinpractice.domain.product.entity.ProductInfo
import com.example.springbootkotlinpractice.domain.product.repository.ProductInfoRepository
import com.example.springbootkotlinpractice.enums.JoinProvider
import com.example.springbootkotlinpractice.enums.OrderStatus
import com.example.springbootkotlinpractice.enums.ResponseCodeEnum
import com.example.springbootkotlinpractice.exception.ApiErrorException
import com.fasterxml.jackson.databind.ObjectMapper
import java.time.LocalDate
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
import org.springframework.test.web.servlet.post

private const val BASE_URL = "/api/v1/payments"
private const val PRODUCT_TOTAL_PRICE = 20_000
private const val DELIVERY_PRICE = 3_000
private const val TOTAL_AMOUNT = PRODUCT_TOTAL_PRICE + DELIVERY_PRICE
private const val FAKE_PAYMENT_KEY = "fake-payment-key"
private const val ORIGINAL_STOCK_COUNT = 3
private const val ORDER_ITEM_COUNT = 1

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("결제 API 통합 테스트")
class PaymentControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var memberRepository: MemberRepository

    @Autowired
    lateinit var deliveryInfoRepository: DeliveryInfoRepository

    @Autowired
    lateinit var productInfoRepository: ProductInfoRepository

    @Autowired
    lateinit var orderInfoRepository: OrderInfoRepository

    @Autowired
    lateinit var orderDetailInfoRepository: OrderDetailInfoRepository

    @Autowired
    lateinit var payInfoRepository: PayInfoRepository

    @Autowired
    lateinit var jwtTokenProvider: JwtTokenProvider

    @MockitoBean
    lateinit var tossPaymentsApi: TossPaymentsApi

    private lateinit var member: Member
    private lateinit var accessToken: String
    private lateinit var product: ProductInfo
    private lateinit var order: OrderInfo

    @BeforeEach
    fun setUp() {
        orderDetailInfoRepository.deleteAll()
        payInfoRepository.deleteAll()
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
                email = "payments-test@example.com",
                password = "encoded-password",
                joinProvider = JoinProvider.EMAIL,
            )
        )
        accessToken = jwtTokenProvider.createAccessToken(member.id, member.email, member.joinProvider, member.role)

        val deliveryInfo = deliveryInfoRepository.save(DeliveryInfo(name = "기본 배송", price = DELIVERY_PRICE))
        // 재고 3개 중 1개가 이 주문 생성 시점에 이미 차감된 상태(2개 남음)를 흉내낸다.
        product = productInfoRepository.save(
            ProductInfo(name = "테스트 상품", price = PRODUCT_TOTAL_PRICE, stockCount = ORIGINAL_STOCK_COUNT - ORDER_ITEM_COUNT)
        )
        order = orderInfoRepository.save(
            OrderInfo.of(
                memberId = member.id,
                productTotalPrice = PRODUCT_TOTAL_PRICE,
                deliveryInfoId = deliveryInfo.id,
                deliveryPrice = DELIVERY_PRICE,
            )
        )
        orderDetailInfoRepository.save(
            OrderDetailInfo.of(
                orderInfo = order,
                productInfo = product,
                price = PRODUCT_TOTAL_PRICE.toLong(),
                count = ORDER_ITEM_COUNT,
            )
        )
    }

    private fun authHeader(token: String = accessToken) = "Bearer $token"

    private fun confirmRequestBody(amount: Int = TOTAL_AMOUNT, orderId: String = order.orderUid): String {
        return objectMapper.writeValueAsString(
            PaymentConfirmRequest(paymentKey = FAKE_PAYMENT_KEY, orderId = orderId, amount = amount)
        )
    }

    private fun stubTossConfirmSuccess(status: String = "DONE") {
        given(
            tossPaymentsApi.confirmPayment(
                TossConfirmPaymentRequest(
                    paymentKey = FAKE_PAYMENT_KEY,
                    orderId = order.orderUid,
                    amount = TOTAL_AMOUNT,
                )
            )
        ).willReturn(
            TossConfirmPaymentResponse(
                paymentKey = FAKE_PAYMENT_KEY,
                orderId = order.orderUid,
                status = status,
                totalAmount = TOTAL_AMOUNT,
                method = "카드",
                approvedAt = "2026-07-19T12:00:00+09:00",
            )
        )
    }

    private fun stubTossConfirmFailure() {
        given(
            tossPaymentsApi.confirmPayment(
                TossConfirmPaymentRequest(
                    paymentKey = FAKE_PAYMENT_KEY,
                    orderId = order.orderUid,
                    amount = TOTAL_AMOUNT,
                )
            )
        ).willThrow(ApiErrorException(ResponseCodeEnum.PAYMENT_CONFIRM_FAILED))
    }

    @Test
    @DisplayName("정상 결제 승인 시 결제 정보가 저장되고 주문이 결제 완료 상태가 된다")
    fun `정상 결제 승인 시 결제 정보가 저장된다`() {
        stubTossConfirmSuccess()

        mockMvc.post("$BASE_URL/confirm") {
            header(HttpHeaders.AUTHORIZATION, authHeader())
            contentType = MediaType.APPLICATION_JSON
            content = confirmRequestBody()
        }.andExpect {
            status { isOk() }
        }

        assertThat(payInfoRepository.existsByOrderInfoId(order.id)).isTrue()
        assertThat(orderInfoRepository.findById(order.id).get().status).isEqualTo(OrderStatus.PAID)
    }

    @Test
    @DisplayName("금액 불일치 시 결제 승인이 실패하고 주문 상태는 그대로 유지된다")
    fun `금액 불일치 시 결제 승인이 실패한다`() {
        mockMvc.post("$BASE_URL/confirm") {
            header(HttpHeaders.AUTHORIZATION, authHeader())
            contentType = MediaType.APPLICATION_JSON
            content = confirmRequestBody(amount = TOTAL_AMOUNT + 1)
        }.andExpect {
            status { isBadRequest() }
        }

        assertThat(payInfoRepository.existsByOrderInfoId(order.id)).isFalse()
        assertThat(orderInfoRepository.findById(order.id).get().status).isEqualTo(OrderStatus.PENDING_PAYMENT)
    }

    @Test
    @DisplayName("이미 결제된 주문 재요청 시 409를 반환한다")
    fun `이미 결제된 주문 재요청 시 409를 반환한다`() {
        stubTossConfirmSuccess()

        mockMvc.post("$BASE_URL/confirm") {
            header(HttpHeaders.AUTHORIZATION, authHeader())
            contentType = MediaType.APPLICATION_JSON
            content = confirmRequestBody()
        }.andExpect { status { isOk() } }

        mockMvc.post("$BASE_URL/confirm") {
            header(HttpHeaders.AUTHORIZATION, authHeader())
            contentType = MediaType.APPLICATION_JSON
            content = confirmRequestBody()
        }.andExpect {
            status { isConflict() }
        }
    }

    @Test
    @DisplayName("존재하지 않는 주문으로 결제 승인 시 404를 반환한다")
    fun `존재하지 않는 주문으로 결제 승인 시 404를 반환한다`() {
        mockMvc.post("$BASE_URL/confirm") {
            header(HttpHeaders.AUTHORIZATION, authHeader())
            contentType = MediaType.APPLICATION_JSON
            content = confirmRequestBody(orderId = "not-exist-order-uid")
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    @DisplayName("다른 유저의 주문 결제 승인 시 403을 반환한다")
    fun `다른 유저의 주문 결제 승인 시 403을 반환한다`() {
        val otherMember = memberRepository.save(
            Member.of(
                lastName = "김",
                firstName = "철수",
                birthDate = LocalDate.of(1999, 1, 1),
                phoneNumber = "010-9999-0000",
                email = "other-payments-test@example.com",
                password = "encoded-password",
                joinProvider = JoinProvider.EMAIL,
            )
        )
        val otherToken = jwtTokenProvider.createAccessToken(
            otherMember.id, otherMember.email, otherMember.joinProvider, otherMember.role
        )

        mockMvc.post("$BASE_URL/confirm") {
            header(HttpHeaders.AUTHORIZATION, authHeader(otherToken))
            contentType = MediaType.APPLICATION_JSON
            content = confirmRequestBody()
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    @DisplayName("Toss 승인 실패 시 502를 반환하고 주문이 취소되며 재고가 복구된다")
    fun `Toss 승인 실패 시 주문이 취소되고 재고가 복구된다`() {
        stubTossConfirmFailure()

        val result = mockMvc.post("$BASE_URL/confirm") {
            header(HttpHeaders.AUTHORIZATION, authHeader())
            contentType = MediaType.APPLICATION_JSON
            content = confirmRequestBody()
        }.andReturn()

        assertThat(result.response.status).isEqualTo(502)
        assertThat(payInfoRepository.existsByOrderInfoId(order.id)).isFalse()
        assertThat(orderInfoRepository.findById(order.id).get().status).isEqualTo(OrderStatus.CANCELLED)
        assertThat(productInfoRepository.findById(product.id).get().stockCount).isEqualTo(ORIGINAL_STOCK_COUNT)
    }

    @Test
    @DisplayName("취소된 주문으로 재시도하면 409를 반환하고 Toss를 다시 호출하지 않는다")
    fun `취소된 주문으로 재시도하면 409를 반환한다`() {
        stubTossConfirmFailure()

        val firstResult = mockMvc.post("$BASE_URL/confirm") {
            header(HttpHeaders.AUTHORIZATION, authHeader())
            contentType = MediaType.APPLICATION_JSON
            content = confirmRequestBody()
        }.andReturn()
        assertThat(firstResult.response.status).isEqualTo(502)

        mockMvc.post("$BASE_URL/confirm") {
            header(HttpHeaders.AUTHORIZATION, authHeader())
            contentType = MediaType.APPLICATION_JSON
            content = confirmRequestBody()
        }.andExpect {
            status { isConflict() }
        }
    }
}
