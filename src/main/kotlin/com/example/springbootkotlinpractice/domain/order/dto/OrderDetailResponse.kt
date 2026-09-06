package com.example.springbootkotlinpractice.domain.order.dto

import com.example.springbootkotlinpractice.enums.OrderStatus
import com.fasterxml.jackson.annotation.JsonProperty

data class OrderDetailResponse(
    val orderId: Long,
    val orderUid: String,
    val productTotalPrice: Int,
    val deliveryPrice: Int,
    val totalPrice: Int,
    val status: OrderStatus,
    @get:JsonProperty("isPaid")
    val isPaid: Boolean,
    val itemList: List<OrderItemResponse>,
)

data class OrderItemResponse(
    val productOptionId: Long,
    val productId: Long,
    val productName: String,
    val optionName: String,
    val price: Long,
    val count: Int,
)
