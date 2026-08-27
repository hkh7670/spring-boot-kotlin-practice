package com.example.springbootkotlinpractice.domain.order.service

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

        order.cancelPaidOrder()
        payment.cancel()
        orderStatusHistoryRepository.save(OrderStatusHistory.of(order.id, order.status))

        orderItemRepository.findByOrder(order).forEach {
            productOptionRepository.increaseStock(it.productOption.id, it.count)
        }
    }
}
