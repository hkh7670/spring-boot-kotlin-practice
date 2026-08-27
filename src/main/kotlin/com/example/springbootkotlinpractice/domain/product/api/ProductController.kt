package com.example.springbootkotlinpractice.domain.product.api

import com.example.springbootkotlinpractice.common.dto.CommonResponse
import com.example.springbootkotlinpractice.common.dto.PageResponse
import com.example.springbootkotlinpractice.common.dto.ResponseHandler
import com.example.springbootkotlinpractice.domain.product.dto.ProductDetailResponse
import com.example.springbootkotlinpractice.domain.product.dto.ProductSummaryResponse
import com.example.springbootkotlinpractice.domain.product.service.ProductService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.PageRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "[PRODUCT] Product", description = "상품 목록 / 검색 / 상세 조회 API")
@RequestMapping("/api/v1/products")
@RestController
class ProductController(
    private val productService: ProductService,
) {

    @Operation(
        summary = "상품 목록 조회 API",
        description = "카테고리(categoryId)/검색어(keyword) 조건으로 상품 목록을 페이징 조회한다.",
    )
    @GetMapping
    fun getProducts(
        @RequestParam(required = false) categoryId: Long?,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<CommonResponse<PageResponse<ProductSummaryResponse>>> {
        val response = productService.getProducts(categoryId, keyword, PageRequest.of(page, size))
        return ResponseHandler.ok(response)
    }

    @Operation(
        summary = "상품 상세 조회 API",
        description = "상품 상세 조회 API",
    )
    @GetMapping("/{productId}")
    fun getProduct(
        @PathVariable productId: Long,
    ): ResponseEntity<CommonResponse<ProductDetailResponse>> {
        val response = productService.getProduct(productId)
        return ResponseHandler.ok(response)
    }
}
