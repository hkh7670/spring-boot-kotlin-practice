package com.example.springbootkotlinpractice.domain.order.dto

import com.example.springbootkotlinpractice.enums.OrderStatus

data class OrderSummaryResponse(
    val orderId: Long,
    val orderUid: String,
    val status: OrderStatus,
    val totalPrice: Int,
    val representativeProductName: String,
    val itemCount: Int,
)
