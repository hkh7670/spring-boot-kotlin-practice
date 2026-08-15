package com.example.springbootkotlinpractice.common.payment.toss

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.service.annotation.PostExchange

interface TossPaymentsApi {

    @PostExchange("/v1/payments/confirm")
    fun confirmPayment(@RequestBody request: TossConfirmPaymentRequest): TossConfirmPaymentResponse

    @PostExchange("/v1/payments/{paymentKey}/cancel")
    fun cancelPayment(
        @PathVariable paymentKey: String,
        @RequestBody request: TossCancelPaymentRequest,
    ): TossCancelPaymentResponse
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

data class TossCancelPaymentRequest(
    val cancelReason: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TossCancelPaymentResponse(
    val paymentKey: String,
    val orderId: String,
    val status: String,
)
