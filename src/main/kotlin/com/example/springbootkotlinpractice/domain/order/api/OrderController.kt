package com.example.springbootkotlinpractice.domain.order.api

import com.example.springbootkotlinpractice.common.dto.CommonResponse
import com.example.springbootkotlinpractice.common.dto.ResponseHandler
import com.example.springbootkotlinpractice.common.security.UserPrincipal
import com.example.springbootkotlinpractice.domain.order.dto.OrderCancelResponse
import com.example.springbootkotlinpractice.domain.order.dto.OrderCreateRequest
import com.example.springbootkotlinpractice.domain.order.dto.OrderCreateResponse
import com.example.springbootkotlinpractice.domain.order.dto.OrderDetailResponse
import com.example.springbootkotlinpractice.domain.order.service.OrderCancelService
import com.example.springbootkotlinpractice.domain.order.service.OrderService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RequestMapping("/api/v1/orders")
@RestController
class OrderController(
    private val orderService: OrderService,
    private val orderCancelService: OrderCancelService,
) {

    @PreAuthorize("hasRole('USER')")
    @PostMapping
    fun createOrder(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @Valid @RequestBody request: OrderCreateRequest,
    ): ResponseEntity<CommonResponse<OrderCreateResponse>> {
        val response = orderService.createOrder(userPrincipal.id, request)
        return ResponseHandler.created(response)
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/{orderId}")
    fun getOrder(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable orderId: Long,
    ): ResponseEntity<CommonResponse<OrderDetailResponse>> {
        val response = orderService.getOrder(userPrincipal.id, orderId)
        return ResponseHandler.ok(response)
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/{orderId}/cancel")
    fun cancelOrder(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable orderId: Long,
    ): ResponseEntity<CommonResponse<OrderCancelResponse>> {
        val response = orderCancelService.cancelOrder(userPrincipal.id, orderId)
        return ResponseHandler.ok(response)
    }
}
