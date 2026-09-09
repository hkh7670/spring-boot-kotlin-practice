package com.example.springbootkotlinpractice.domain.search.dto

data class ProductAutocompleteResponse(
    val productId: Long,
    val name: String,
    val imageUrl: String?,
)
