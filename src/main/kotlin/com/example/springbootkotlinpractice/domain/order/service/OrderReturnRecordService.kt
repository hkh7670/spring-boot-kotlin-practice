package com.example.springbootkotlinpractice.domain.order.service

import com.example.springbootkotlinpractice.domain.order.dto.OrderStatusResponse
import com.example.springbootkotlinpractice.domain.order.entity.OrderStatusHistory
import com.example.springbootkotlinpractice.domain.order.repository.OrderItemRepository
import com.example.springbootkotlinpractice.domain.order.repository.OrderRepository
import com.example.springbootkotlinpractice.domain.order.repository.OrderStatusHistoryRepository
import com.example.springbootkotlinpractice.domain.payment.repository.PaymentRepository
import com.example.springbootkotlinpractice.domain.product.repository.ProductOptionRepository
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

        order.completeReturn()
        payment.cancel()
        orderStatusHistoryRepository.save(OrderStatusHistory.of(order.id, order.status))

        orderItemRepository.findByOrder(order).forEach {
            productOptionRepository.increaseStock(it.productOption.id, it.count)
        }

        return OrderStatusResponse(orderId = order.id, orderUid = order.orderUid, status = order.status)
    }
}
