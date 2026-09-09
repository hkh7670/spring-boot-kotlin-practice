package com.example.springbootkotlinpractice.domain.search.repository

import com.example.springbootkotlinpractice.domain.search.document.ProductDocument

interface ProductSearchRepository {
    fun ensureIndexExists()

    fun bulkIndex(documents: List<ProductDocument>): Int

    fun suggest(keyword: String, size: Int): List<ProductDocument>
}
