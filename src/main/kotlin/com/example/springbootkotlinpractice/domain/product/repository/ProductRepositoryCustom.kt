package com.example.springbootkotlinpractice.domain.product.repository

import com.example.springbootkotlinpractice.domain.product.entity.Product
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface ProductRepositoryCustom {

    // 카테고리/키워드 조건으로 상품을 검색한다 (null 인 조건은 무시)
    fun search(categoryId: Long?, keyword: String?, pageable: Pageable): Page<Product>
}
