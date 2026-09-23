package com.example.springbootkotlinpractice.domain.search.repository

import com.example.springbootkotlinpractice.domain.search.document.ProductDocument

interface ProductSearchRepository {
    fun ensureIndexExists()

    fun bulkIndex(documents: List<ProductDocument>): Int

    fun index(document: ProductDocument)

    fun delete(productId: Long)

    // 존재하지 않는 문서는 무시한다(재색인 때 이미 지워진 삭제 상품 문서를 다시 정리하는 경우)
    fun bulkDelete(productIds: List<Long>)

    fun suggest(keyword: String, size: Int): List<ProductDocument>
}
