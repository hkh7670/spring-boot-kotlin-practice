package com.example.springbootkotlinpractice.domain.product.dto

data class ProductSummaryResponse(
    val id: Long,
    val name: String,
    val price: Int,
    val imageUrl: String?,
    val stockCount: Int,
    val categoryId: Long?,
    val vendorName: String?,
)

data class ProductDetailResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val imageUrl: String?,
    val productOptions: List<ProductOptionResponse>,
    val categoryId: Long?,
    val categoryName: String?,
    val vendorId: Long?,
    val vendorName: String?,
)

data class ProductOptionResponse(
    val id: Long,
    val name: String,
    val price: Int,
    val stockCount: Int,
)
