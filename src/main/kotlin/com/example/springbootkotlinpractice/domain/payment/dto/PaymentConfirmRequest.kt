package com.example.springbootkotlinpractice.domain.payment.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive

data class PaymentConfirmRequest(
    @field:Schema(description = "Toss 결제 승인 시 발급되는 결제 키", example = "5EnNZRJGvaBX7zk2yd8ydk6q")
    @field:NotBlank
    val paymentKey: String,

    @field:Schema(description = "가맹점 주문번호 (order_info.order_uid)", example = "01J8ZX9K5Q7N3F2W1V6C4T8B0M")
    @field:NotBlank
    val orderId: String,

    @field:Schema(description = "결제 금액", example = "50000")
    @field:Positive
    val amount: Int,
)
