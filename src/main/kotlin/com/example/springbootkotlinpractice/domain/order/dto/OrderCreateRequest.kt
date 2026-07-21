package com.example.springbootkotlinpractice.domain.order.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Positive

data class OrderCreateRequest(
    @field:Schema(description = "배송 옵션 ID", example = "1")
    val deliveryOptionId: Long,

    @field:Schema(description = "주문할 상품 목록")
    @field:NotEmpty
    @field:Valid
    val items: List<OrderItemRequest>,
)

data class OrderItemRequest(
    @field:Schema(description = "상품 ID", example = "1")
    val productId: Long,

    @field:Schema(description = "주문 수량", example = "2")
    @field:Positive
    val count: Int,
)
