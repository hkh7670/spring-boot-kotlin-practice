package com.example.springbootkotlinpractice.domain.order.api

import com.example.springbootkotlinpractice.common.dto.CommonResponse
import com.example.springbootkotlinpractice.common.dto.ResponseHandler
import com.example.springbootkotlinpractice.domain.order.dto.OrderStatusResponse
import com.example.springbootkotlinpractice.domain.order.service.OrderReturnService
import com.example.springbootkotlinpractice.domain.order.service.OrderShippingService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@Tag(name = "[ADMIN] Order", description = "관리자 배송 / 반품처리 API")
@RequestMapping("/api/v1/admin/orders")
@RestController
class AdminOrderController(
    private val orderShippingService: OrderShippingService,
    private val orderReturnService: OrderReturnService,
) {

    @Operation(
        summary = "배송 시작 처리 API",
        description = "결제완료(PAID) 상태의 주문을 배송중(SHIPPING) 상태로 전환한다.",
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{orderId}/ship")
    fun markShipping(@PathVariable orderId: Long): ResponseEntity<CommonResponse<OrderStatusResponse>> {
        return ResponseHandler.ok(orderShippingService.markShipping(orderId))
    }

    @Operation(
        summary = "배송 완료 처리 API",
        description = "배송중(SHIPPING) 상태의 주문을 배송완료(DELIVERED) 상태로 전환한다.",
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{orderId}/deliver")
    fun markDelivered(@PathVariable orderId: Long): ResponseEntity<CommonResponse<OrderStatusResponse>> {
        return ResponseHandler.ok(orderShippingService.markDelivered(orderId))
    }

    @Operation(
        summary = "반품 완료 처리 API",
        description = "반품 택배 도착을 확인한 뒤 반품완료 처리한다. Toss 환불 API 호출이 성공한 경우에만 DB에 반영된다.",
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{orderId}/returns/complete")
    fun completeReturn(@PathVariable orderId: Long): ResponseEntity<CommonResponse<OrderStatusResponse>> {
        return ResponseHandler.ok(orderReturnService.completeReturn(orderId))
    }
}
