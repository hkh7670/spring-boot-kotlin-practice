package com.example.springbootkotlinpractice.domain.order.dto

import com.example.springbootkotlinpractice.enums.OrderStatus

data class OrderStatusResponse(
    val orderId: Long,
    val orderUid: String,
    val status: OrderStatus,
)
