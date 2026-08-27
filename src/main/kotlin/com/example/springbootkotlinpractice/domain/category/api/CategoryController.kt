package com.example.springbootkotlinpractice.domain.category.api

import com.example.springbootkotlinpractice.common.dto.CommonResponse
import com.example.springbootkotlinpractice.common.dto.ResponseHandler
import com.example.springbootkotlinpractice.domain.category.dto.CategoryResponse
import com.example.springbootkotlinpractice.domain.category.service.CategoryService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "[CATEGORY] Category", description = "카테고리 목록 조회 API")
@RequestMapping("/api/v1/categories")
@RestController
class CategoryController(
    private val categoryService: CategoryService,
) {

    @Operation(
        summary = "카테고리 목록 조회 API",
        description = "전체 카테고리(대/중/소분류)를 계층 정보(parentId)와 함께 조회한다.",
    )
    @GetMapping
    fun getCategories(): ResponseEntity<CommonResponse<List<CategoryResponse>>> {
        val response = categoryService.getCategories()
        return ResponseHandler.ok(response)
    }
}
