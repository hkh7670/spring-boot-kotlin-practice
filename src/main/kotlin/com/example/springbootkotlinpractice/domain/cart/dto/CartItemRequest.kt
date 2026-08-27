package com.example.springbootkotlinpractice.domain.cart.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

data class CartItemAddRequest(
    @field:Schema(description = "상품 옵션 ID", example = "1")
    @field:NotNull
    val productOptionId: Long,

    @field:Schema(description = "담을 수량", example = "1")
    @field:Positive
    val count: Int,
)

data class CartItemCountRequest(
    @field:Schema(description = "변경할 수량", example = "2")
    @field:Positive
    val count: Int,
)
