package com.example.springbootkotlinpractice.domain.order.service

import com.example.springbootkotlinpractice.domain.delivery.entity.DeliveryInfo
import com.example.springbootkotlinpractice.domain.delivery.repository.DeliveryInfoRepository
import com.example.springbootkotlinpractice.domain.order.dto.OrderCreateRequest
import com.example.springbootkotlinpractice.domain.order.dto.OrderCreateResponse
import com.example.springbootkotlinpractice.domain.order.dto.OrderDetailResponse
import com.example.springbootkotlinpractice.domain.order.dto.OrderItemResponse
import com.example.springbootkotlinpractice.domain.order.entity.OrderDetailInfo
import com.example.springbootkotlinpractice.domain.order.entity.OrderInfo
import com.example.springbootkotlinpractice.domain.order.entity.OrderStatusHistory
import com.example.springbootkotlinpractice.domain.order.repository.OrderDetailInfoRepository
import com.example.springbootkotlinpractice.domain.order.repository.OrderInfoRepository
import com.example.springbootkotlinpractice.domain.order.repository.OrderStatusHistoryRepository
import com.example.springbootkotlinpractice.domain.product.entity.ProductInfo
import com.example.springbootkotlinpractice.domain.product.repository.ProductInfoRepository
import com.example.springbootkotlinpractice.enums.OrderStatus
import com.example.springbootkotlinpractice.enums.ResponseCodeEnum
import com.example.springbootkotlinpractice.exception.ApiErrorException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OrderService(
    private val orderInfoRepository: OrderInfoRepository,
    private val orderDetailInfoRepository: OrderDetailInfoRepository,
    private val orderStatusHistoryRepository: OrderStatusHistoryRepository,
    private val productInfoRepository: ProductInfoRepository,
    private val deliveryInfoRepository: DeliveryInfoRepository,
) {

    @Transactional
    fun createOrder(memberId: Long, request: OrderCreateRequest): OrderCreateResponse {
        val deliveryInfo = getDeliveryInfo(request.deliveryInfoId)
        val orderItems = request.items.map { item -> getProduct(item.productId) to item.count }

        orderItems.forEach { (product, count) ->
            val updatedRowCount = productInfoRepository.decreaseStock(product.id, count)
            if (updatedRowCount == 0) {
                throw ApiErrorException(ResponseCodeEnum.NOT_ENOUGH_STOCK)
            }
        }

        val productTotalPrice = orderItems.sumOf { (product, count) -> product.price * count }

        val savedOrder = orderInfoRepository.save(
            OrderInfo.of(
                memberId = memberId,
                productTotalPrice = productTotalPrice,
                deliveryInfoId = deliveryInfo.id,
                deliveryPrice = deliveryInfo.price,
            )
        )

        orderDetailInfoRepository.saveAll(
            orderItems.map { (product, count) ->
                OrderDetailInfo.of(
                    orderInfo = savedOrder,
                    productInfo = product,
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
            deliveryPrice = deliveryInfo.price,
            totalPrice = productTotalPrice + deliveryInfo.price,
        )
    }

    fun getOrder(memberId: Long, orderId: Long): OrderDetailResponse {
        val order = orderInfoRepository.findByIdAndMemberId(orderId, memberId)
            ?: throw ApiErrorException(ResponseCodeEnum.NOT_FOUND_ORDER)
        val orderDetails = orderDetailInfoRepository.findByOrderInfoId(order.id)

        return OrderDetailResponse(
            orderId = order.id,
            orderUid = order.orderUid,
            productTotalPrice = order.productTotalPrice,
            deliveryPrice = order.deliveryPrice,
            totalPrice = order.productTotalPrice + order.deliveryPrice,
            status = order.status,
            isPaid = order.status == OrderStatus.PAID,
            itemList = orderDetails.map {
                OrderItemResponse(
                    productId = it.productInfo.id,
                    productName = it.productInfo.name,
                    price = it.price,
                    count = it.count
                )
            },
        )
    }

    private fun getDeliveryInfo(deliveryInfoId: Long): DeliveryInfo {
        return deliveryInfoRepository.findByIdOrNull(deliveryInfoId)
            ?: throw ApiErrorException(ResponseCodeEnum.NOT_FOUND_DELIVERY)
    }

    private fun getProduct(productId: Long): ProductInfo {
        return productInfoRepository.findByIdOrNull(productId)
            ?: throw ApiErrorException(ResponseCodeEnum.NOT_FOUND_PRODUCT)
    }
}
