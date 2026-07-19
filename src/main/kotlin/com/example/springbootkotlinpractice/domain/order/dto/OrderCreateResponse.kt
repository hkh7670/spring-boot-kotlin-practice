package com.example.springbootkotlinpractice.domain.order.dto

data class OrderCreateResponse(
    val orderId: Long,
    val orderUid: String,
    val productTotalPrice: Int,
    val deliveryPrice: Int,
    val totalPrice: Int,
)
