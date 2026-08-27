package com.example.springbootkotlinpractice.domain.payment.service

import com.example.springbootkotlinpractice.common.payment.toss.TossConfirmPaymentResponse
import com.example.springbootkotlinpractice.domain.order.entity.OrderStatusHistory
import com.example.springbootkotlinpractice.domain.order.event.OrderCancelledEvent
import com.example.springbootkotlinpractice.domain.order.event.OrderPaidEvent
import com.example.springbootkotlinpractice.domain.order.repository.OrderItemRepository
import com.example.springbootkotlinpractice.domain.order.repository.OrderRepository
import com.example.springbootkotlinpractice.domain.order.repository.OrderStatusHistoryRepository
import com.example.springbootkotlinpractice.domain.payment.dto.PaymentConfirmResponse
import com.example.springbootkotlinpractice.domain.payment.entity.Payment
import com.example.springbootkotlinpractice.domain.payment.repository.PaymentRepository
import com.example.springbootkotlinpractice.domain.product.repository.ProductOptionRepository
import com.example.springbootkotlinpractice.enums.OrderStatus
import com.example.springbootkotlinpractice.enums.PaymentStatus
import com.example.springbootkotlinpractice.enums.ResponseCodeEnum
import com.example.springbootkotlinpractice.exception.ApiErrorException
import java.time.OffsetDateTime
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

// Toss API 호출과 분리된, 결제 확정/취소 결과를 DB에 기록하는 책임만 담당한다.
// 각 메서드가 별도 트랜잭션이라 Toss 호출(느릴 수 있는 외부 네트워크 요청) 동안 DB 커넥션을 붙잡고 있지 않는다.
@Service
class PaymentRecordService(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val orderStatusHistoryRepository: OrderStatusHistoryRepository,
    private val paymentRepository: PaymentRepository,
    private val productOptionRepository: ProductOptionRepository,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {

    @Transactional
    fun completePayment(
        orderId: Long,
        tossResponse: TossConfirmPaymentResponse
    ): PaymentConfirmResponse {
        val order = orderRepository.findByIdOrNull(orderId)
            ?: throw ApiErrorException(ResponseCodeEnum.NOT_FOUND_ORDER)

        // 동시 중복 confirm 요청 방어 — 원자적 조건부 UPDATE로 PENDING_PAYMENT 상태일 때만 전이시킨다.
        // 0건이면 이미 다른 요청이 먼저 처리한 것이므로 Payment 중복 생성/이벤트 중복 발행을 막는다.
        val updatedRows = orderRepository.updateStatusIfCurrent(orderId, OrderStatus.PENDING_PAYMENT.name, OrderStatus.PAID.name)
        if (updatedRows == 0) {
            throw ApiErrorException(ResponseCodeEnum.ALREADY_PAID_ORDER)
        }
        orderStatusHistoryRepository.save(
            OrderStatusHistory.of(order.id, OrderStatus.PAID)
        )

        val payment = paymentRepository.save(
            Payment.of(
                orderId = order.id,
                paymentKey = tossResponse.paymentKey,
                amount = tossResponse.totalAmount,
                status = PaymentStatus.valueOf(tossResponse.status),
                method = tossResponse.method,
                approvedAt = tossResponse.approvedAt?.let {
                    OffsetDateTime.parse(it).toLocalDateTime()
                },
            )
        )

        applicationEventPublisher.publishEvent(
            OrderPaidEvent(
                orderId = order.id,
                orderUid = order.orderUid,
                memberId = order.memberId,
                amount = payment.amount
            )
        )

        return PaymentConfirmResponse(
            paymentId = payment.id,
            orderId = order.id,
            orderUid = order.orderUid,
            paymentKey = payment.paymentKey,
            amount = payment.amount,
            status = payment.status,
            method = payment.method,
            approvedAt = payment.approvedAt,
        )
    }

    @Transactional
    fun cancelOrderAndRestoreStock(orderId: Long) {
        val order = orderRepository.findByIdOrNull(orderId)
            ?: throw ApiErrorException(ResponseCodeEnum.NOT_FOUND_ORDER)

        // 동시 중복 confirm-실패 요청 방어(더블클릭, React StrictMode 이중 마운트 등) — 원자적 조건부
        // UPDATE로 PENDING_PAYMENT 상태일 때만 취소 처리한다. 0건이면 이미 다른 요청이 취소+재고복구를
        // 끝냈다는 뜻이므로 재고 중복 복구를 막기 위해 조용히 종료한다.
        val updatedRows = orderRepository.updateStatusIfCurrent(orderId, OrderStatus.PENDING_PAYMENT.name, OrderStatus.CANCELLED.name)
        if (updatedRows == 0) {
            return
        }
        orderStatusHistoryRepository.save(OrderStatusHistory.of(order.id, OrderStatus.CANCELLED))

        orderItemRepository.findByOrder(order).forEach {
            productOptionRepository.increaseStock(it.productOption.id, it.count)
        }

        applicationEventPublisher.publishEvent(
            OrderCancelledEvent(
                orderId = order.id,
                orderUid = order.orderUid,
                memberId = order.memberId
            )
        )
    }
}
