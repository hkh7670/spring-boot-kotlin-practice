package com.example.springbootkotlinpractice.domain.order.service

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

// 결제 완료(PAID) 주문 취소의 DB 기록만 담당한다. Toss 취소 API 호출(OrderCancelService)이 이미 성공한
// 뒤에만 호출되므로, 별도의 보정(rollback) 로직이 필요 없다.
@Service
class OrderCancelRecordService(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val orderStatusHistoryRepository: OrderStatusHistoryRepository,
    private val paymentRepository: PaymentRepository,
    private val productOptionRepository: ProductOptionRepository,
) {

    @Transactional
    fun markCancelled(orderId: Long) {
        val order = orderRepository.findByIdOrNull(orderId)
            ?: throw ApiErrorException(ResponseCodeEnum.NOT_FOUND_ORDER)
        val payment = paymentRepository.findByOrderId(orderId)
            ?: throw ApiErrorException(ResponseCodeEnum.NOT_FOUND_PAYMENT_INFO)

        // 동시 중복 취소 요청 방어 — 원자적 조건부 UPDATE로 PAID 상태일 때만 취소 처리한다. 0건이면
        // 이미 다른 요청이 처리했다는 뜻이므로(Toss 취소 자체는 이미 성공한 뒤라 보정 불필요) 재고
        // 중복 복구를 막기 위해 조용히 종료한다.
        val updatedRows = orderRepository.updateStatusIfCurrent(orderId, OrderStatus.PAID.name, OrderStatus.CANCELLED.name)
        if (updatedRows == 0) {
            return
        }

        payment.cancel()
        orderStatusHistoryRepository.save(OrderStatusHistory.of(order.id, OrderStatus.CANCELLED))

        orderItemRepository.findByOrder(order).forEach {
            productOptionRepository.increaseStock(it.productOption.id, it.count)
        }
    }
}
