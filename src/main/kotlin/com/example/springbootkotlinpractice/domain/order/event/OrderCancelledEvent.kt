package com.example.springbootkotlinpractice.domain.order.event

data class OrderCancelledEvent(
    val orderId: Long,
    val orderUid: String,
    val memberId: Long,
)
