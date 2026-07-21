package com.example.springbootkotlinpractice.domain.payment.service

import com.example.springbootkotlinpractice.common.payment.toss.TossConfirmPaymentRequest
import com.example.springbootkotlinpractice.common.payment.toss.TossConfirmPaymentResponse
import com.example.springbootkotlinpractice.common.payment.toss.TossPaymentsApi
import com.example.springbootkotlinpractice.domain.order.entity.Order
import com.example.springbootkotlinpractice.domain.order.repository.OrderRepository
import com.example.springbootkotlinpractice.domain.payment.dto.PaymentConfirmRequest
import com.example.springbootkotlinpractice.domain.payment.dto.PaymentConfirmResponse
import com.example.springbootkotlinpractice.enums.OrderStatus
import com.example.springbootkotlinpractice.enums.ResponseCodeEnum
import com.example.springbootkotlinpractice.exception.ApiErrorException
import org.springframework.stereotype.Service

// Toss API 호출(외부 네트워크 요청)과 DB 쓰기(PaymentRecordService)를 분리한다.
// 트랜잭션 안에서 외부 API를 호출하면 응답이 느릴 때 DB 커넥션을 그만큼 오래 붙잡게 되어 커넥션 풀 고갈 위험이 있다.
@Service
class PaymentService(
    private val orderRepository: OrderRepository,
    private val paymentRecordService: PaymentRecordService,
    private val tossPaymentsApi: TossPaymentsApi,
) {

    fun confirmPayment(memberId: Long, request: PaymentConfirmRequest): PaymentConfirmResponse {
        val order = getOrder(request.orderId)
        validateOwner(order, memberId)
        validateOrderStatus(order)

        val expectedAmount = calculateExpectedAmount(order)
        validateAmount(expectedAmount, request.amount)

        val tossResponse = confirmToss(order.id, request)
        validateAmount(expectedAmount, tossResponse.totalAmount)

        return paymentRecordService.completePayment(order.id, tossResponse)
    }

    private fun getOrder(orderUid: String): Order {
        return orderRepository.findByOrderUid(orderUid)
            ?: throw ApiErrorException(ResponseCodeEnum.NOT_FOUND_ORDER)
    }

    private fun validateOwner(order: Order, memberId: Long) {
        if (order.memberId != memberId) {
            throw ApiErrorException(ResponseCodeEnum.ACCESS_DENIED)
        }
    }

    private fun validateOrderStatus(order: Order) {
        when (order.status) {
            OrderStatus.PAID -> throw ApiErrorException(ResponseCodeEnum.ALREADY_PAID_ORDER)
            OrderStatus.CANCELLED -> throw ApiErrorException(ResponseCodeEnum.ORDER_ALREADY_CANCELLED)
            OrderStatus.PENDING_PAYMENT -> Unit
        }
    }

    private fun calculateExpectedAmount(order: Order): Int {
        return order.productTotalPrice + order.deliveryPrice
    }

    private fun validateAmount(expectedAmount: Int, actualAmount: Int) {
        if (expectedAmount != actualAmount) {
            throw ApiErrorException(ResponseCodeEnum.PAYMENT_AMOUNT_MISMATCH)
        }
    }

    // Toss 승인이 명확히 거절되면 주문을 취소하고 재고를 복구한 뒤 예외를 던진다.
    // cancelOrderAndRestoreStock() 은 별도 빈의 독립된 트랜잭션이라, 이후 예외를 던져도 그 커밋은 영향받지 않는다.
    private fun confirmToss(
        orderId: Long,
        request: PaymentConfirmRequest
    ): TossConfirmPaymentResponse {
        return runCatching {
            tossPaymentsApi.confirmPayment(
                TossConfirmPaymentRequest(
                    paymentKey = request.paymentKey,
                    orderId = request.orderId,
                    amount = request.amount,
                )
            )
        }.getOrElse {
            paymentRecordService.cancelOrderAndRestoreStock(orderId)
            if (it is ApiErrorException) {
                throw it
            }
            throw ApiErrorException(ResponseCodeEnum.PAYMENT_CONFIRM_FAILED)
        }
    }
}
