package com.example.springbootkotlinpractice.domain.category.service

import com.example.springbootkotlinpractice.domain.category.dto.CategoryResponse
import com.example.springbootkotlinpractice.domain.category.repository.CategoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CategoryService(
    private val categoryRepository: CategoryRepository,
) {

    @Transactional(readOnly = true)
    fun getCategories(): List<CategoryResponse> {
        return categoryRepository.findAll().map {
            CategoryResponse(
                id = it.id,
                parentId = it.parentId,
                name = it.name,
                level = it.level,
            )
        }
    }
}
