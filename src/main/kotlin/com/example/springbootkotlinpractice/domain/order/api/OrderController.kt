package com.example.springbootkotlinpractice.domain.order.api

import com.example.springbootkotlinpractice.common.dto.CommonResponse
import com.example.springbootkotlinpractice.common.dto.ResponseHandler
import com.example.springbootkotlinpractice.common.security.UserPrincipal
import com.example.springbootkotlinpractice.domain.order.dto.OrderCancelResponse
import com.example.springbootkotlinpractice.domain.order.dto.OrderCreateRequest
import com.example.springbootkotlinpractice.domain.order.dto.OrderCreateResponse
import com.example.springbootkotlinpractice.domain.order.dto.OrderDetailResponse
import com.example.springbootkotlinpractice.domain.order.dto.OrderStatusResponse
import com.example.springbootkotlinpractice.domain.order.service.OrderCancelService
import com.example.springbootkotlinpractice.domain.order.service.OrderReturnService
import com.example.springbootkotlinpractice.domain.order.service.OrderService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@Tag(name = "[ORDER] Order", description = "주문 생성 / 조회 / 취소 / 반품요청 API")
@RequestMapping("/api/v1/orders")
@RestController
class OrderController(
    private val orderService: OrderService,
    private val orderCancelService: OrderCancelService,
    private val orderReturnService: OrderReturnService,
) {

    @Operation(
        summary = "주문 생성 API",
        description = "주문 생성 API",
    )
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "처리 성공"),
    )
    @PreAuthorize("hasRole('USER')")
    @PostMapping
    fun createOrder(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @Valid @RequestBody request: OrderCreateRequest,
    ): ResponseEntity<CommonResponse<OrderCreateResponse>> {
        val response = orderService.createOrder(userPrincipal.id, request)
        return ResponseHandler.created(response)
    }

    @Operation(
        summary = "주문 상세 조회 API",
        description = "주문 상세 조회 API",
    )
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/{orderId}")
    fun getOrder(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable orderId: Long,
    ): ResponseEntity<CommonResponse<OrderDetailResponse>> {
        val response = orderService.getOrder(userPrincipal.id, orderId)
        return ResponseHandler.ok(response)
    }

    @Operation(
        summary = "주문 취소 API",
        description = "결제완료(PAID) 상태까지의 주문만 취소할 수 있다. 배송이 시작된 이후에는 반품 절차를 이용해야 한다.",
    )
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/{orderId}/cancel")
    fun cancelOrder(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable orderId: Long,
    ): ResponseEntity<CommonResponse<OrderCancelResponse>> {
        val response = orderCancelService.cancelOrder(userPrincipal.id, orderId)
        return ResponseHandler.ok(response)
    }

    @Operation(
        summary = "반품 요청 API",
        description = "배송완료(DELIVERED) 상태의 주문에 대해 반품을 요청한다. 실제 반품완료 처리는 운영자가 진행한다.",
    )
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/{orderId}/return")
    fun requestReturn(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable orderId: Long,
    ): ResponseEntity<CommonResponse<OrderStatusResponse>> {
        val response = orderReturnService.requestReturn(userPrincipal.id, orderId)
        return ResponseHandler.ok(response)
    }
}
