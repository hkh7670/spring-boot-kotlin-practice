package com.example.springbootkotlinpractice.domain.product.service

import com.example.springbootkotlinpractice.domain.product.dto.AdminProductCreateRequest
import com.example.springbootkotlinpractice.domain.product.dto.AdminProductOptionRequest
import com.example.springbootkotlinpractice.domain.product.dto.AdminProductOptionUpdateRequest
import com.example.springbootkotlinpractice.domain.product.dto.AdminProductUpdateRequest
import com.example.springbootkotlinpractice.domain.product.dto.ProductDetailResponse
import com.example.springbootkotlinpractice.domain.search.service.ProductSearchService
import org.springframework.stereotype.Service

// @Transactional 없음: DB 쓰기는 AdminProductRecordService의 트랜잭션에서 커밋을 마치고,
// 그 뒤에 검색 색인(OpenSearch I/O)과 응답 조회를 트랜잭션 밖에서 수행한다.
@Service
class AdminProductService(
    private val adminProductRecordService: AdminProductRecordService,
    private val productService: ProductService,
    private val productSearchService: ProductSearchService,
) {

    fun createProduct(request: AdminProductCreateRequest): ProductDetailResponse {
        val product = adminProductRecordService.createProduct(request)
        productSearchService.indexProduct(product)
        return productService.getProduct(product.id)
    }

    fun updateProduct(productId: Long, request: AdminProductUpdateRequest): ProductDetailResponse {
        val product = adminProductRecordService.updateProduct(productId, request)
        productSearchService.indexProduct(product)
        return productService.getProduct(product.id)
    }

    fun deleteProduct(productId: Long) {
        adminProductRecordService.deleteProduct(productId)
        productSearchService.removeProduct(productId)
    }

    fun addOption(productId: Long, request: AdminProductOptionRequest): ProductDetailResponse {
        adminProductRecordService.addOption(productId, request)
        return productService.getProduct(productId)
    }

    fun updateOption(
        productId: Long,
        optionId: Long,
        request: AdminProductOptionUpdateRequest,
    ): ProductDetailResponse {
        adminProductRecordService.updateOption(productId, optionId, request)
        return productService.getProduct(productId)
    }
}
