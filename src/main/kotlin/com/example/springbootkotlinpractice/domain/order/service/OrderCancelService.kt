package com.example.springbootkotlinpractice.domain.order.service

import com.example.springbootkotlinpractice.common.payment.toss.TossPaymentCanceller
import com.example.springbootkotlinpractice.domain.order.dto.OrderCancelResponse
import com.example.springbootkotlinpractice.domain.order.entity.Order
import com.example.springbootkotlinpractice.domain.order.event.OrderCancelledEvent
import com.example.springbootkotlinpractice.domain.order.event.OrderEventPublisher
import com.example.springbootkotlinpractice.domain.order.repository.OrderRepository
import com.example.springbootkotlinpractice.domain.payment.entity.Payment
import com.example.springbootkotlinpractice.domain.payment.repository.PaymentRepository
import com.example.springbootkotlinpractice.enums.OrderStatus
import com.example.springbootkotlinpractice.enums.ResponseCodeEnum
import com.example.springbootkotlinpractice.exception.ApiErrorException
import org.springframework.stereotype.Service

// Toss 취소 API 호출(외부 네트워크 요청)을 먼저 하고, 성공했을 때만 DB에 기록한다(OrderCancelRecordService).
// 재고 복구를 포함한 DB 쓰기를 Toss 취소 확정 이전에 해버리면, 그 사이에 다른 주문이 복구된 재고를
// 가져갈 수 있어 재고 정합성이 깨질 위험이 있다 — Toss 취소가 실패해 되돌려야 할 때 그 재고를 다시
// 가져오지 못할 수 있기 때문이다. Toss 호출을 먼저 해서 이 문제를 원천적으로 없앤다.
@Service
class OrderCancelService(
    private val orderRepository: OrderRepository,
    private val paymentRepository: PaymentRepository,
    private val orderCancelRecordService: OrderCancelRecordService,
    private val tossPaymentCanceller: TossPaymentCanceller,
    private val orderEventPublisher: OrderEventPublisher,
) {

    companion object {
        private const val CANCEL_REASON = "고객 요청에 의한 주문 취소"
    }

    fun cancelOrder(memberId: Long, orderId: Long): OrderCancelResponse {
        val order = getOrder(memberId, orderId)
        validateOrderIsPaid(order)
        val payment = getPayment(order.id)

        // Toss 결제 취소 API 호출
        tossPaymentCanceller.cancel(payment.paymentKey, CANCEL_REASON)

        // 결제 취소완료 후 DB 반영 (재고 원복)
        orderCancelRecordService.markCancelled(order.id)

        // Kafka 이벤트 발행 (주문 취소)
        orderEventPublisher.publish(
            OrderCancelledEvent(
                orderId = order.id,
                orderUid = order.orderUid,
                memberId = order.memberId
            )
        )

        return OrderCancelResponse(
            orderId = order.id,
            orderUid = order.orderUid,
            status = OrderStatus.CANCELLED
        )
    }

    private fun getOrder(memberId: Long, orderId: Long): Order {
        return orderRepository.findByIdAndMemberId(orderId, memberId)
            ?: throw ApiErrorException(ResponseCodeEnum.NOT_FOUND_ORDER)
    }

    // Payment 유무를 조회하기 전에 먼저 주문 상태부터 확인한다. 결제 전 주문은 Payment 레코드 자체가
    // 없어서, 이 검증이 없으면 "결제 전이라 취소 불가"가 아니라 "결제 정보 없음"으로 잘못 응답하게 된다.
    private fun validateOrderIsPaid(order: Order) {
        when (order.status) {
            OrderStatus.PAID -> Unit
            OrderStatus.CANCELLED -> throw ApiErrorException(ResponseCodeEnum.ORDER_ALREADY_CANCELLED)
            OrderStatus.PENDING_PAYMENT -> throw ApiErrorException(ResponseCodeEnum.ORDER_NOT_PAID)
            OrderStatus.SHIPPING, OrderStatus.DELIVERED,
            OrderStatus.RETURNING, OrderStatus.RETURNED -> throw ApiErrorException(ResponseCodeEnum.ORDER_ALREADY_SHIPPING)
        }
    }

    private fun getPayment(orderId: Long): Payment {
        return paymentRepository.findByOrderId(orderId)
            ?: throw ApiErrorException(ResponseCodeEnum.NOT_FOUND_PAYMENT_INFO)
    }
}
