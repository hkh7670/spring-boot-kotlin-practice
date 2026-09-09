package com.example.springbootkotlinpractice.domain.search.repository

import com.example.springbootkotlinpractice.common.Logging
import com.example.springbootkotlinpractice.domain.search.document.ProductDocument
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.mapping.TypeMapping
import org.opensearch.client.opensearch.core.bulk.BulkOperation
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Repository

@Repository
class ProductSearchRepositoryImpl(
    private val openSearchClient: OpenSearchClient,
) : ProductSearchRepository, Logging {

    override fun ensureIndexExists() {
        val exists = openSearchClient.indices().exists { it.index(ProductDocument.INDEX_NAME) }.value()
        if (exists) return

        val mapper = openSearchClient._transport().jsonpMapper()
        val mapping = ClassPathResource(MAPPING_RESOURCE_PATH).inputStream.use { inputStream ->
            val parser = mapper.jsonProvider().createParser(inputStream)
            mapper.deserialize(parser, TypeMapping::class.java)
        }
        openSearchClient.indices().create { it.index(ProductDocument.INDEX_NAME).mappings(mapping) }
    }

    override fun bulkIndex(documents: List<ProductDocument>): Int {
        if (documents.isEmpty()) return 0

        val operations = documents.map { document ->
            BulkOperation.of { operation ->
                operation.index<ProductDocument> { index ->
                    index.index(ProductDocument.INDEX_NAME)
                        .id(document.productId.toString())
                        .document(document)
                }
            }
        }
        val response = openSearchClient.bulk { it.operations(operations) }
        // 개별 실패 항목은 로그만 남기고 계속 진행 — 관리자가 재실행 가능한 멱등 작업이므로 전체 중단 안 함
        if (response.errors()) {
            response.items().filter { it.error() != null }
                .forEach { item -> logger.error("상품 색인 실패 productId=${item.id()}: ${item.error()?.reason()}") }
        }
        return response.items().count { it.error() == null }
    }

    override fun suggest(keyword: String, size: Int): List<ProductDocument> {
        val response = openSearchClient.search(
            { search ->
                search.index(ProductDocument.INDEX_NAME)
                    .size(0)
                    .suggest { suggester ->
                        suggester.text(keyword)
                            .suggesters(SUGGESTION_NAME) { fieldSuggester ->
                                fieldSuggester.completion { completion ->
                                    completion.field(SUGGEST_FIELD_NAME)
                                        .skipDuplicates(true)
                                        .size(size)
                                }
                            }
                    }
            },
            ProductDocument::class.java,
        )
        return response.suggest()[SUGGESTION_NAME]
            ?.firstOrNull { it.isCompletion }
            ?.completion()
            ?.options()
            ?.mapNotNull { it.source() }
            ?: emptyList()
    }

    companion object {
        private const val MAPPING_RESOURCE_PATH = "opensearch/product-autocomplete-mapping.json"
        private const val SUGGESTION_NAME = "product-suggest"
        private const val SUGGEST_FIELD_NAME = "nameSuggest"
    }
}
