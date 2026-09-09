package com.example.springbootkotlinpractice.domain.search.service

import com.example.springbootkotlinpractice.common.Logging
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
                val chunk = productRepository.findAll(PageRequest.of(page, batchSize))
                if (chunk.isEmpty) break
                totalIndexed += productSearchRepository.bulkIndex(chunk.content.map { ProductDocument.of(it) })
                if (!chunk.hasNext()) break
                page++
            }
            return ReindexResponse(indexedCount = totalIndexed)
        } catch (e: Exception) {
            logger.error("상품 재색인 실패", e)
            throw ApiErrorException(ResponseCodeEnum.SEARCH_ENGINE_ERROR)
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
