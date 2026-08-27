package com.example.springbootkotlinpractice.domain.delivery.api

import com.example.springbootkotlinpractice.common.dto.CommonResponse
import com.example.springbootkotlinpractice.common.dto.ResponseHandler
import com.example.springbootkotlinpractice.domain.delivery.dto.DeliveryOptionResponse
import com.example.springbootkotlinpractice.domain.delivery.service.DeliveryOptionService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "[DELIVERY] Delivery Option", description = "배송 옵션 목록 조회 API")
@RequestMapping("/api/v1/delivery-options")
@RestController
class DeliveryOptionController(
    private val deliveryOptionService: DeliveryOptionService,
) {

    @Operation(
        summary = "배송 옵션 목록 조회 API",
        description = "배송 옵션 목록 조회 API",
    )
    @GetMapping
    fun getDeliveryOptions(): ResponseEntity<CommonResponse<List<DeliveryOptionResponse>>> {
        val response = deliveryOptionService.getDeliveryOptions()
        return ResponseHandler.ok(response)
    }
}
