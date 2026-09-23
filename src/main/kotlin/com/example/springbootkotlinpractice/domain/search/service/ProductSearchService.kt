package com.example.springbootkotlinpractice.domain.search.service

import com.example.springbootkotlinpractice.common.Logging
import com.example.springbootkotlinpractice.domain.product.entity.Product
import com.example.springbootkotlinpractice.domain.product.repository.ProductRepository
import com.example.springbootkotlinpractice.domain.search.document.ProductDocument
import com.example.springbootkotlinpractice.domain.search.dto.ProductAutocompleteResponse
import com.example.springbootkotlinpractice.domain.search.dto.ReindexResponse
import com.example.springbootkotlinpractice.domain.search.repository.ProductSearchRepository
import com.example.springbootkotlinpractice.enums.ResponseCodeEnum
import com.example.springbootkotlinpractice.exception.ApiErrorException
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

@Service
class ProductSearchService(
    private val productRepository: ProductRepository,
    private val productSearchRepository: ProductSearchRepository,
) : Logging {

    private val batchSize = 500

    // @Transactional 없음 — productRepository.findAll(pageable)이 페이지 단위로 짧은 read-only
    // 트랜잭션을 열고 닫으므로, "DB 짧은 읽기 → OpenSearch 느린 I/O(트랜잭션 밖)" 반복 구조가
    // self-invocation 함정 없이 자연스럽게 만들어진다.
    fun reindexAll(): ReindexResponse {
        try {
            productSearchRepository.ensureIndexExists()
            var page = 0
            var totalIndexed = 0
            while (true) {
                val chunk = productRepository.findAllByIsDeletedFalse(PageRequest.of(page, batchSize))
                if (chunk.isEmpty) break
                totalIndexed += productSearchRepository.bulkIndex(chunk.content.map { ProductDocument.of(it) })
                if (!chunk.hasNext()) break
                page++
            }
            removeDeletedProductsFromIndex()
            return ReindexResponse(indexedCount = totalIndexed)
        } catch (e: Exception) {
            logger.error("상품 재색인 실패", e)
            throw ApiErrorException(ResponseCodeEnum.SEARCH_ENGINE_ERROR)
        }
    }

    // soft delete된 상품 문서를 인덱스에서 정리한다. 단건 삭제(removeProduct)가 실패해 남은 문서를
    // 재색인으로 복구할 수 있게 하는 부분이다.
    private fun removeDeletedProductsFromIndex() {
        var page = 0
        while (true) {
            val chunk = productRepository.findAllByIsDeletedTrue(PageRequest.of(page, batchSize))
            if (chunk.isEmpty) break
            productSearchRepository.bulkDelete(chunk.content.map { it.id })
            if (!chunk.hasNext()) break
            page++
        }
    }

    // 관리자 상품 생성/수정을 색인에 반영한다. 이미 커밋된 상품 API를 색인 실패로 깨뜨리면 안 되므로
    // 로그만 남기고, 누락분은 관리자 재색인으로 복구한다. 인덱스가 없을 때 동적 매핑으로 자동 생성되어
    // completion 매핑이 깨지는 것을 막으려고 색인 전에 인덱스 존재를 보장한다.
    fun indexProduct(product: Product) {
        try {
            productSearchRepository.ensureIndexExists()
            productSearchRepository.index(ProductDocument.of(product))
        } catch (e: Exception) {
            logger.warn("상품 색인 갱신 실패, 재색인으로 복구 필요: productId=${product.id}", e)
        }
    }

    fun removeProduct(productId: Long) {
        try {
            productSearchRepository.delete(productId)
        } catch (e: Exception) {
            logger.warn("상품 색인 삭제 실패, 재색인으로 복구 필요: productId=$productId", e)
        }
    }

    // 자동완성은 진행성 향상 기능 — OpenSearch 장애가 검색창 타이핑을 500으로 깨뜨리면 안 되므로
    // 조용히 빈 리스트로 대체한다.
    fun autocomplete(keyword: String, size: Int): List<ProductAutocompleteResponse> {
        if (keyword.isBlank()) {
            throw ApiErrorException(ResponseCodeEnum.BAD_REQUEST)
        }
        return try {
            productSearchRepository.suggest(keyword, size).map {
                ProductAutocompleteResponse(productId = it.productId, name = it.name, imageUrl = it.imageUrl)
            }
        } catch (e: Exception) {
            logger.warn("자동완성 조회 실패, 빈 결과로 대체: keyword=$keyword", e)
            emptyList()
        }
    }
}
