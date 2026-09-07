package com.example.springbootkotlinpractice.domain.order.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

data class OrderCreateRequest(
    @field:NotNull
    @field:Schema(description = "배송 옵션 ID", example = "1")
    val deliveryOptionId: Long,

    @field:Schema(description = "주문할 상품 목록")
    @field:NotEmpty
    @field:Valid
    val items: List<OrderItemRequest>,

    @field:Schema(description = "사용할 보유 쿠폰 ID (member_coupons.id, 미사용 시 null)", example = "1", nullable = true)
    val memberCouponId: Long? = null,

    @field:Schema(description = "사용할 포인트 금액 (미사용 시 0)", example = "1000")
    val usePointAmount: Int = 0,
)

data class OrderItemRequest(
    @field:Schema(description = "상품 옵션 ID", example = "1")
    val productOptionId: Long,

    @field:Schema(description = "주문 수량", example = "2")
    @field:Positive
    val count: Int,
)
