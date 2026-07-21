package com.example.springbootkotlinpractice.domain.order.service

import com.example.springbootkotlinpractice.domain.delivery.entity.DeliveryOption
import com.example.springbootkotlinpractice.domain.delivery.repository.DeliveryOptionRepository
import com.example.springbootkotlinpractice.domain.order.dto.OrderCreateRequest
import com.example.springbootkotlinpractice.domain.order.dto.OrderCreateResponse
import com.example.springbootkotlinpractice.domain.order.dto.OrderDetailResponse
import com.example.springbootkotlinpractice.domain.order.dto.OrderItemResponse
import com.example.springbootkotlinpractice.domain.order.entity.Order
import com.example.springbootkotlinpractice.domain.order.entity.OrderItem
import com.example.springbootkotlinpractice.domain.order.entity.OrderStatusHistory
import com.example.springbootkotlinpractice.domain.order.repository.OrderItemRepository
import com.example.springbootkotlinpractice.domain.order.repository.OrderRepository
import com.example.springbootkotlinpractice.domain.order.repository.OrderStatusHistoryRepository
import com.example.springbootkotlinpractice.domain.product.entity.Product
import com.example.springbootkotlinpractice.domain.product.repository.ProductRepository
import com.example.springbootkotlinpractice.enums.OrderStatus
import com.example.springbootkotlinpractice.enums.ResponseCodeEnum
import com.example.springbootkotlinpractice.exception.ApiErrorException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val orderStatusHistoryRepository: OrderStatusHistoryRepository,
    private val productRepository: ProductRepository,
    private val deliveryOptionRepository: DeliveryOptionRepository,
) {

    @Transactional
    fun createOrder(memberId: Long, request: OrderCreateRequest): OrderCreateResponse {
        val deliveryOption = getDeliveryOption(request.deliveryOptionId)
        val orderItems = request.items.map { item -> getProduct(item.productId) to item.count }

        orderItems.forEach { (product, count) ->
            val updatedRowCount = productRepository.decreaseStock(product.id, count)
            if (updatedRowCount == 0) {
                throw ApiErrorException(ResponseCodeEnum.NOT_ENOUGH_STOCK)
            }
        }

        val productTotalPrice = orderItems.sumOf { (product, count) -> product.price * count }

        val savedOrder = orderRepository.save(
            Order.of(
                memberId = memberId,
                productTotalPrice = productTotalPrice,
                deliveryOptionId = deliveryOption.id,
                deliveryPrice = deliveryOption.price,
            )
        )

        orderItemRepository.saveAll(
            orderItems.map { (product, count) ->
                OrderItem.of(
                    order = savedOrder,
                    product = product,
                    price = product.price.toLong(),
                    count = count,
                )
            }
        )

        orderStatusHistoryRepository.save(OrderStatusHistory.of(savedOrder.id, savedOrder.status))

        return OrderCreateResponse(
            orderId = savedOrder.id,
            orderUid = savedOrder.orderUid,
            productTotalPrice = productTotalPrice,
            deliveryPrice = deliveryOption.price,
            totalPrice = productTotalPrice + deliveryOption.price,
        )
    }

    fun getOrder(memberId: Long, orderId: Long): OrderDetailResponse {
        val order = orderRepository.findByIdAndMemberId(orderId, memberId)
            ?: throw ApiErrorException(ResponseCodeEnum.NOT_FOUND_ORDER)
        val orderItems = orderItemRepository.findByOrderId(order.id)

        return OrderDetailResponse(
            orderId = order.id,
            orderUid = order.orderUid,
            productTotalPrice = order.productTotalPrice,
            deliveryPrice = order.deliveryPrice,
            totalPrice = order.productTotalPrice + order.deliveryPrice,
            status = order.status,
            isPaid = order.status == OrderStatus.PAID,
            itemList = orderItems.map {
                OrderItemResponse(
                    productId = it.product.id,
                    productName = it.product.name,
                    price = it.price,
                    count = it.count
                )
            },
        )
    }

    private fun getDeliveryOption(deliveryOptionId: Long): DeliveryOption {
        return deliveryOptionRepository.findByIdOrNull(deliveryOptionId)
            ?: throw ApiErrorException(ResponseCodeEnum.NOT_FOUND_DELIVERY)
    }

    private fun getProduct(productId: Long): Product {
        return productRepository.findByIdOrNull(productId)
            ?: throw ApiErrorException(ResponseCodeEnum.NOT_FOUND_PRODUCT)
    }
}
