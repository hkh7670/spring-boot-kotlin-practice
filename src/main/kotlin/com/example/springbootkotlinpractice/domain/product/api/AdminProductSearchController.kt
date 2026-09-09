package com.example.springbootkotlinpractice.domain.product.api

import com.example.springbootkotlinpractice.common.dto.CommonResponse
import com.example.springbootkotlinpractice.common.dto.ResponseHandler
import com.example.springbootkotlinpractice.domain.search.dto.ReindexResponse
import com.example.springbootkotlinpractice.domain.search.service.ProductSearchService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "[ADMIN] Product Search", description = "관리자 상품 자동완성 색인 관리 API")
@RequestMapping("/api/v1/admin/products")
@RestController
class AdminProductSearchController(
    private val productSearchService: ProductSearchService,
) {

    @Operation(
        summary = "상품 자동완성 재색인 API",
        description = "DB의 전체 상품을 읽어 OpenSearch에 벌크 색인한다.",
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/reindex")
    fun reindex(): ResponseEntity<CommonResponse<ReindexResponse>> {
        return ResponseHandler.ok(productSearchService.reindexAll())
    }
}
