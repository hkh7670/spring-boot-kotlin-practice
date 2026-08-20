package com.example.springbootkotlinpractice.domain.order.service

import com.example.springbootkotlinpractice.domain.order.dto.OrderStatusResponse
import com.example.springbootkotlinpractice.domain.order.entity.Order
import com.example.springbootkotlinpractice.domain.order.entity.OrderStatusHistory
import com.example.springbootkotlinpractice.domain.order.repository.OrderRepository
import com.example.springbootkotlinpractice.domain.order.repository.OrderStatusHistoryRepository
import com.example.springbootkotlinpractice.enums.ResponseCodeEnum
import com.example.springbootkotlinpractice.exception.ApiErrorException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

// 배송 상태 전환(운영자 전용). 외부 연동 없이 DB 상태만 바꾸는 단순 전환이라, Toss 호출이 끼는
// 결제취소/반품완료와 달리 별도 트랜잭션 분리가 필요 없다.
@Service
class OrderShippingService(
    private val orderRepository: OrderRepository,
    private val orderStatusHistoryRepository: OrderStatusHistoryRepository,
) {

    @Transactional
    fun markShipping(orderId: Long): OrderStatusResponse {
        val order = getOrder(orderId)
        order.markShipping()
        orderStatusHistoryRepository.save(OrderStatusHistory.of(order.id, order.status))
        return OrderStatusResponse(orderId = order.id, orderUid = order.orderUid, status = order.status)
    }

    @Transactional
    fun markDelivered(orderId: Long): OrderStatusResponse {
        val order = getOrder(orderId)
        order.markDelivered()
        orderStatusHistoryRepository.save(OrderStatusHistory.of(order.id, order.status))
        return OrderStatusResponse(orderId = order.id, orderUid = order.orderUid, status = order.status)
    }

    private fun getOrder(orderId: Long): Order {
        return orderRepository.findByIdOrNull(orderId)
            ?: throw ApiErrorException(ResponseCodeEnum.NOT_FOUND_ORDER)
    }
}
