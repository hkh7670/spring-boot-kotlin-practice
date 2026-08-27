package com.example.springbootkotlinpractice.domain.cart.dto

data class CartItemResponse(
    val productOptionId: Long,
    val productId: Long,
    val productName: String,
    val optionName: String,
    val price: Int,
    val imageUrl: String?,
    val count: Int,
    val soldOut: Boolean,
)

data class CartResponse(
    val items: List<CartItemResponse>,
)
