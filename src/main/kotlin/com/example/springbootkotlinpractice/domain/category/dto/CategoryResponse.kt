package com.example.springbootkotlinpractice.domain.category.dto

import com.example.springbootkotlinpractice.enums.CategoryLevel

data class CategoryResponse(
    val id: Long,
    val parentId: Long?,
    val name: String,
    val level: CategoryLevel,
)
