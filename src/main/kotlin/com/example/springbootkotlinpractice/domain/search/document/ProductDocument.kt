package com.example.springbootkotlinpractice.domain.search.document

import com.example.springbootkotlinpractice.domain.product.entity.Product

data class ProductDocument(
    val productId: Long,
    val name: String,
    val nameSuggest: NameSuggestField,
    val imageUrl: String?,
    val categoryId: Long?,
) {
    data class NameSuggestField(
        val input: List<String>,
        val weight: Int = 1,
    )

    companion object {
        const val INDEX_NAME = "product_autocomplete"

        // Completion Suggester는 문자열 맨 앞에서만 prefix 매칭하므로, 상품명 전체뿐 아니라
        // 공백 기준 토큰들도 함께 넣어 중간 단어부터 타이핑해도 추천되게 한다.
        fun of(product: Product): ProductDocument {
            val tokens = (listOf(product.name) + product.name.split(Regex("\\s+")))
                .filter { it.isNotBlank() }
                .distinct()
            return ProductDocument(
                productId = product.id,
                name = product.name,
                nameSuggest = NameSuggestField(input = tokens),
                imageUrl = product.imageUrl,
                categoryId = product.categoryId,
            )
        }
    }
}
