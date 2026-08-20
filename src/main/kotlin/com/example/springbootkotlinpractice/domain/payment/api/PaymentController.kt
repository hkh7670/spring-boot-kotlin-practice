package com.example.springbootkotlinpractice.domain.payment.api

import com.example.springbootkotlinpractice.common.dto.CommonResponse
import com.example.springbootkotlinpractice.common.dto.ResponseHandler
import com.example.springbootkotlinpractice.common.security.UserPrincipal
import com.example.springbootkotlinpractice.domain.payment.dto.PaymentConfirmRequest
import com.example.springbootkotlinpractice.domain.payment.dto.PaymentConfirmResponse
import com.example.springbootkotlinpractice.domain.payment.service.PaymentService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "[PAYMENT] Payment", description = "결제 승인 API")
@RequestMapping("/api/v1/payments")
@RestController
class PaymentController(
    private val paymentService: PaymentService,
) {

    @Operation(
        summary = "결제 승인 API",
        description = "Toss Payments 결제 승인 API를 호출하고, 성공 시에만 주문을 결제완료(PAID) 상태로 전환한다.",
    )
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/confirm")
    fun confirmPayment(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @Valid @RequestBody request: PaymentConfirmRequest,
    ): ResponseEntity<CommonResponse<PaymentConfirmResponse>> {
        val response = paymentService.confirmPayment(userPrincipal.id, request)
        return ResponseHandler.ok(response)
    }
}
