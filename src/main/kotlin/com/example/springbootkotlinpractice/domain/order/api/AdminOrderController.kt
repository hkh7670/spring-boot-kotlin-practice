package com.example.springbootkotlinpractice.domain.order.api

import com.example.springbootkotlinpractice.common.dto.CommonResponse
import com.example.springbootkotlinpractice.common.dto.ResponseHandler
import com.example.springbootkotlinpractice.domain.order.dto.OrderStatusResponse
import com.example.springbootkotlinpractice.domain.order.service.OrderReturnService
import com.example.springbootkotlinpractice.domain.order.service.OrderShippingService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RequestMapping("/api/v1/admin/orders")
@RestController
class AdminOrderController(
    private val orderShippingService: OrderShippingService,
    private val orderReturnService: OrderReturnService,
) {

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{orderId}/ship")
    fun markShipping(@PathVariable orderId: Long): ResponseEntity<CommonResponse<OrderStatusResponse>> {
        return ResponseHandler.ok(orderShippingService.markShipping(orderId))
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{orderId}/deliver")
    fun markDelivered(@PathVariable orderId: Long): ResponseEntity<CommonResponse<OrderStatusResponse>> {
        return ResponseHandler.ok(orderShippingService.markDelivered(orderId))
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{orderId}/returns/complete")
    fun completeReturn(@PathVariable orderId: Long): ResponseEntity<CommonResponse<OrderStatusResponse>> {
        return ResponseHandler.ok(orderReturnService.completeReturn(orderId))
    }
}
