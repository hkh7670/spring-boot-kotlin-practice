package com.example.springbootkotlinpractice.domain.payment.dto

import com.example.springbootkotlinpractice.enums.PaymentStatus
import java.time.LocalDateTime

data class PaymentConfirmResponse(
    val payInfoId: Long,
    val orderId: Long,
    val orderUid: String,
    val paymentKey: String,
    val amount: Int,
    val status: PaymentStatus,
    val method: String?,
    val approvedAt: LocalDateTime?,
)
