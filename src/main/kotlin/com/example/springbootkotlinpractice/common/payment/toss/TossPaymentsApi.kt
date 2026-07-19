package com.example.springbootkotlinpractice.common.payment.toss

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.service.annotation.PostExchange

interface TossPaymentsApi {

    @PostExchange("/v1/payments/confirm")
    fun confirmPayment(@RequestBody request: TossConfirmPaymentRequest): TossConfirmPaymentResponse
}

data class TossConfirmPaymentRequest(
    val paymentKey: String,
    val orderId: String,
    val amount: Int,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TossConfirmPaymentResponse(
    val paymentKey: String,
    val orderId: String,
    val status: String,
    val totalAmount: Int,
    val method: String?,
    val approvedAt: String?,
)
