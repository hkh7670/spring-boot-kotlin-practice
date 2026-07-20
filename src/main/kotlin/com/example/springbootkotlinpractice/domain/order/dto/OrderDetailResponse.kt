package com.example.springbootkotlinpractice.domain.order.dto

import com.example.springbootkotlinpractice.enums.OrderStatus

data class OrderDetailResponse(
    val orderId: Long,
    val orderUid: String,
    val productTotalPrice: Int,
    val deliveryPrice: Int,
    val totalPrice: Int,
    val status: OrderStatus,
    val isPaid: Boolean,
    val itemList: List<OrderItemResponse>,
)

data class OrderItemResponse(
    val productId: Long,
    val productName: String,
    val price: Long,
    val count: Int,
)
