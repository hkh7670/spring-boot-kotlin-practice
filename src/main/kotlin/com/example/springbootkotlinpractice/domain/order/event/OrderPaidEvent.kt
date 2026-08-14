package com.example.springbootkotlinpractice.domain.order.event

data class OrderPaidEvent(
    val orderId: Long,
    val orderUid: String,
    val memberId: Long,
    val amount: Int,
)
