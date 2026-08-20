package com.example.springbootkotlinpractice.domain.order.service

import com.example.springbootkotlinpractice.common.payment.toss.TossPaymentCanceller
import com.example.springbootkotlinpractice.domain.order.dto.OrderStatusResponse
import com.example.springbootkotlinpractice.domain.order.entity.Order
import com.example.springbootkotlinpractice.domain.order.entity.OrderStatusHistory
import com.example.springbootkotlinpractice.domain.order.repository.OrderRepository
import com.example.springbootkotlinpractice.domain.order.repository.OrderStatusHistoryRepository
import com.example.springbootkotlinpractice.domain.payment.entity.Payment
import com.example.springbootkotlinpractice.domain.payment.repository.PaymentRepository
import com.example.springbootkotlinpractice.enums.OrderStatus
import com.example.springbootkotlinpractice.enums.ResponseCodeEnum
import com.example.springbootkotlinpractice.exception.ApiErrorException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OrderReturnService(
    private val orderRepository: OrderRepository,
    private val orderStatusHistoryRepository: OrderStatusHistoryRepository,
    private val paymentRepository: PaymentRepository,
    private val orderReturnRecordService: OrderReturnRecordService,
    private val tossPaymentCanceller: TossPaymentCanceller,
) {

    companion object {
        private const val CANCEL_REASON = "반품 완료에 따른 환불"
    }

    // 고객이 배송완료(DELIVERED) 주문에 대해 반품을 요청한다. 외부 호출이 없는 단순 상태 전환이라
    // 별도 트랜잭션 분리가 필요 없다.
    @Transactional
    fun requestReturn(memberId: Long, orderId: Long): OrderStatusResponse {
        val order = orderRepository.findByIdAndMemberId(orderId, memberId)
            ?: throw ApiErrorException(ResponseCodeEnum.NOT_FOUND_ORDER)

        order.requestReturn()
        orderStatusHistoryRepository.save(OrderStatusHistory.of(order.id, order.status))

        return OrderStatusResponse(
            orderId = order.id,
            orderUid = order.orderUid,
            status = order.status
        )
    }

    // 반품 택배가 도착했을 때 운영자가 반품완료 처리한다. Toss 환불 API를 먼저 호출하고 성공했을 때만
    // DB에 반영한다 (OrderCancelService 와 동일한 이유 — 재고 원복을 환불 확정 전에 해버리면 그 사이
    // 다른 주문이 그 재고를 가져갈 수 있다).
    fun completeReturn(orderId: Long): OrderStatusResponse {
        val order = getOrder(orderId)
        validateOrderIsReturning(order)
        val payment = getPayment(order.id)

        tossPaymentCanceller.cancel(payment.paymentKey, CANCEL_REASON)

        // 결제 취소완료 후 DB 반영 (재고 원복)
        return orderReturnRecordService.markReturned(order.id)
    }

    private fun getOrder(orderId: Long): Order {
        return orderRepository.findByIdOrNull(orderId)
            ?: throw ApiErrorException(ResponseCodeEnum.NOT_FOUND_ORDER)
    }

    private fun validateOrderIsReturning(order: Order) {
        if (order.status != OrderStatus.RETURNING) {
            throw ApiErrorException(ResponseCodeEnum.ORDER_NOT_RETURNING)
        }
    }

    private fun getPayment(orderId: Long): Payment {
        return paymentRepository.findByOrderId(orderId)
            ?: throw ApiErrorException(ResponseCodeEnum.NOT_FOUND_PAYMENT_INFO)
    }
}
