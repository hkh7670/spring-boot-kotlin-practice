package com.example.springbootkotlinpractice.domain.order.service

import com.example.springbootkotlinpractice.common.dto.PageResponse
import com.example.springbootkotlinpractice.domain.delivery.entity.DeliveryOption
import com.example.springbootkotlinpractice.domain.delivery.repository.DeliveryOptionRepository
import com.example.springbootkotlinpractice.domain.order.dto.OrderCreateRequest
import com.example.springbootkotlinpractice.domain.order.dto.OrderCreateResponse
import com.example.springbootkotlinpractice.domain.order.dto.OrderDetailResponse
import com.example.springbootkotlinpractice.domain.order.dto.OrderItemResponse
import com.example.springbootkotlinpractice.domain.order.dto.OrderSummaryResponse
import com.example.springbootkotlinpractice.domain.order.entity.Order
import com.example.springbootkotlinpractice.domain.order.entity.OrderItem
import com.example.springbootkotlinpractice.domain.order.entity.OrderStatusHistory
import com.example.springbootkotlinpractice.domain.order.repository.OrderItemRepository
import com.example.springbootkotlinpractice.domain.order.repository.OrderRepository
import com.example.springbootkotlinpractice.domain.order.repository.OrderStatusHistoryRepository
import com.example.springbootkotlinpractice.domain.product.entity.ProductOption
import com.example.springbootkotlinpractice.domain.product.repository.ProductOptionRepository
import com.example.springbootkotlinpractice.enums.OrderStatus
import com.example.springbootkotlinpractice.enums.ResponseCodeEnum
import com.example.springbootkotlinpractice.exception.ApiErrorException
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val orderStatusHistoryRepository: OrderStatusHistoryRepository,
    private val productOptionRepository: ProductOptionRepository,
    private val deliveryOptionRepository: DeliveryOptionRepository,
) {

    @Transactional
    fun createOrder(memberId: Long, request: OrderCreateRequest): OrderCreateResponse {
        val deliveryOption = getDeliveryOption(request.deliveryOptionId)
        val orderItems = request.items.map { item -> getProductOption(item.productOptionId) to item.count }

        orderItems.forEach { (productOption, count) ->
            val updatedRowCount = productOptionRepository.decreaseStock(productOption.id, count)
            if (updatedRowCount == 0) {
                throw ApiErrorException(ResponseCodeEnum.NOT_ENOUGH_STOCK)
            }
        }

        val productTotalPrice = orderItems.sumOf { (productOption, count) -> productOption.price * count }

        val savedOrder = orderRepository.save(
            Order.of(
                memberId = memberId,
                productTotalPrice = productTotalPrice,
                deliveryOptionId = deliveryOption.id,
                deliveryPrice = deliveryOption.price,
            )
        )

        orderItemRepository.saveAll(
            orderItems.map { (productOption, count) ->
                OrderItem.of(
                    order = savedOrder,
                    productOption = productOption,
                    price = productOption.price.toLong(),
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
                    productOptionId = it.productOption.id,
                    productId = it.productOption.product.id,
                    productName = it.productOption.product.name,
                    optionName = it.productOption.name,
                    price = it.price,
                    count = it.count
                )
            },
        )
    }

    @Transactional(readOnly = true)
    fun getOrders(memberId: Long, pageable: Pageable): PageResponse<OrderSummaryResponse> {
        val orders = orderRepository.findByMemberId(memberId, pageable)
        val itemsByOrderId = orderItemRepository.findByOrderIdIn(orders.content.map { it.id })
            .groupBy { it.order.id }

        return PageResponse.of(
            orders.map { order ->
                val items = itemsByOrderId[order.id].orEmpty()
                OrderSummaryResponse(
                    orderId = order.id,
                    orderUid = order.orderUid,
                    status = order.status,
                    totalPrice = order.productTotalPrice + order.deliveryPrice,
                    representativeProductName = items.firstOrNull()?.productOption?.product?.name.orEmpty(),
                    itemCount = items.size,
                )
            }
        )
    }

    private fun getDeliveryOption(deliveryOptionId: Long): DeliveryOption {
        return deliveryOptionRepository.findByIdOrNull(deliveryOptionId)
            ?: throw ApiErrorException(ResponseCodeEnum.NOT_FOUND_DELIVERY_OPTION)
    }

    private fun getProductOption(productOptionId: Long): ProductOption {
        return productOptionRepository.findByIdFetchProduct(productOptionId)
            ?: throw ApiErrorException(ResponseCodeEnum.NOT_FOUND_PRODUCT_OPTION)
    }
}
