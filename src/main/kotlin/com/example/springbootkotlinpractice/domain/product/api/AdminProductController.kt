package com.example.springbootkotlinpractice.domain.product.api

import com.example.springbootkotlinpractice.common.dto.CommonResponse
import com.example.springbootkotlinpractice.common.dto.ResponseHandler
import com.example.springbootkotlinpractice.domain.product.dto.AdminProductCreateRequest
import com.example.springbootkotlinpractice.domain.product.dto.AdminProductOptionRequest
import com.example.springbootkotlinpractice.domain.product.dto.AdminProductOptionUpdateRequest
import com.example.springbootkotlinpractice.domain.product.dto.AdminProductUpdateRequest
import com.example.springbootkotlinpractice.domain.product.dto.ProductDetailResponse
import com.example.springbootkotlinpractice.domain.product.service.AdminProductService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "[ADMIN] Product", description = "관리자 상품 / 상품 옵션 관리 API")
@RequestMapping("/api/v1/admin/products")
@RestController
class AdminProductController(
    private val adminProductService: AdminProductService,
) {

    @Operation(
        summary = "상품 생성 API",
        description = "상품과 옵션(1개 이상)을 함께 생성하고 자동완성 색인에 반영한다.",
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    fun createProduct(
        @Valid @RequestBody request: AdminProductCreateRequest,
    ): ResponseEntity<CommonResponse<ProductDetailResponse>> {
        return ResponseHandler.created(adminProductService.createProduct(request))
    }

    @Operation(
        summary = "상품 기본정보 수정 API",
        description = "상품명, 설명, 이미지, 카테고리, 업체를 요청 값으로 교체한다. null은 값을 비운다.",
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{productId}")
    fun updateProduct(
        @PathVariable productId: Long,
        @Valid @RequestBody request: AdminProductUpdateRequest,
    ): ResponseEntity<CommonResponse<ProductDetailResponse>> {
        return ResponseHandler.ok(adminProductService.updateProduct(productId, request))
    }

    @Operation(
        summary = "상품 삭제 API",
        description = "상품을 soft delete 처리한다. 과거 주문 조회에는 영향이 없고 자동완성 색인에서 제거된다.",
    )
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{productId}")
    fun deleteProduct(@PathVariable productId: Long): ResponseEntity<CommonResponse<Unit>> {
        adminProductService.deleteProduct(productId)
        return ResponseHandler.ok()
    }

    @Operation(
        summary = "상품 옵션 추가 API",
        description = "상품에 옵션을 추가한다. 옵션명은 상품 내에서 중복될 수 없다.",
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{productId}/options")
    fun addOption(
        @PathVariable productId: Long,
        @Valid @RequestBody request: AdminProductOptionRequest,
    ): ResponseEntity<CommonResponse<ProductDetailResponse>> {
        return ResponseHandler.created(adminProductService.addOption(productId, request))
    }

    @Operation(
        summary = "상품 옵션 수정 API",
        description = "옵션명과 가격을 요청 값으로 교체한다. 재고는 값을 보낼 때만 덮어쓰고 null이면 변경하지 않는다. " +
                "옵션 삭제는 제공하지 않으므로 재고를 0으로 두어 품절 처리한다.",
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{productId}/options/{optionId}")
    fun updateOption(
        @PathVariable productId: Long,
        @PathVariable optionId: Long,
        @Valid @RequestBody request: AdminProductOptionUpdateRequest,
    ): ResponseEntity<CommonResponse<ProductDetailResponse>> {
        return ResponseHandler.ok(adminProductService.updateOption(productId, optionId, request))
    }
}
