package com.example.springbootkotlinpractice.domain.order.service

import com.example.springbootkotlinpractice.domain.order.dto.OrderStatusResponse
import com.example.springbootkotlinpractice.domain.order.entity.OrderStatusHistory
import com.example.springbootkotlinpractice.domain.order.repository.OrderItemRepository
import com.example.springbootkotlinpractice.domain.order.repository.OrderRepository
import com.example.springbootkotlinpractice.domain.order.repository.OrderStatusHistoryRepository
import com.example.springbootkotlinpractice.domain.payment.repository.PaymentRepository
import com.example.springbootkotlinpractice.domain.product.repository.ProductOptionRepository
import com.example.springbootkotlinpractice.enums.OrderStatus
import com.example.springbootkotlinpractice.enums.ResponseCodeEnum
import com.example.springbootkotlinpractice.exception.ApiErrorException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

// 반품완료의 DB 기록만 담당한다. Toss 환불 API 호출(OrderReturnService)이 이미 성공한 뒤에만
// 호출되므로 보정(rollback) 로직이 필요 없다 (OrderCancelRecordService 와 동일한 구조).
@Service
class OrderReturnRecordService(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val orderStatusHistoryRepository: OrderStatusHistoryRepository,
    private val paymentRepository: PaymentRepository,
    private val productOptionRepository: ProductOptionRepository,
) {

    @Transactional
    fun markReturned(orderId: Long): OrderStatusResponse {
        val order = orderRepository.findByIdOrNull(orderId)
            ?: throw ApiErrorException(ResponseCodeEnum.NOT_FOUND_ORDER)
        val payment = paymentRepository.findByOrderId(orderId)
            ?: throw ApiErrorException(ResponseCodeEnum.NOT_FOUND_PAYMENT_INFO)

        // 동시 중복 반품완료 요청 방어 — 원자적 조건부 UPDATE로 RETURNING 상태일 때만 처리한다. 0건이면
        // 이미 다른 요청이 처리했다는 뜻(Toss 환불 자체는 이미 성공한 뒤라 보정 불필요) — 최종 상태는
        // 어차피 RETURNED로 동일하므로 에러 없이 같은 응답을 그대로 반환한다.
        val updatedRows = orderRepository.updateStatusIfCurrent(orderId, OrderStatus.RETURNING, OrderStatus.RETURNED)
        if (updatedRows == 0) {
            return OrderStatusResponse(orderId = order.id, orderUid = order.orderUid, status = OrderStatus.RETURNED)
        }

        payment.cancel()
        orderStatusHistoryRepository.save(OrderStatusHistory.of(order.id, OrderStatus.RETURNED))

        orderItemRepository.findByOrder(order).forEach {
            productOptionRepository.increaseStock(it.productOption.id, it.count)
        }

        return OrderStatusResponse(orderId = order.id, orderUid = order.orderUid, status = OrderStatus.RETURNED)
    }
}
