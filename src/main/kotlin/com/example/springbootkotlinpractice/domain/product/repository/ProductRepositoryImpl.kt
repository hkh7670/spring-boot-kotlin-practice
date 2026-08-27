package com.example.springbootkotlinpractice.domain.product.repository

import com.example.springbootkotlinpractice.domain.product.entity.Product
import com.example.springbootkotlinpractice.domain.product.entity.QProduct.product
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable

class ProductRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : ProductRepositoryCustom {

    override fun search(categoryId: Long?, keyword: String?, pageable: Pageable): Page<Product> {
        val total = queryFactory
            .select(product.count())
            .from(product)
            .where(
                categoryIdEq(categoryId),
                keywordContains(keyword),
            )
            .fetchOne() ?: 0L

        if (total == 0L) {
            return PageImpl(emptyList(), pageable, 0L)
        }

        val content = queryFactory
            .selectFrom(product)
            .where(
                categoryIdEq(categoryId),
                keywordContains(keyword),
            )
            .orderBy(product.id.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        return PageImpl(content, pageable, total)
    }

    private fun categoryIdEq(categoryId: Long?): BooleanExpression? {
        return categoryId?.let { product.categoryId.eq(it) }
    }

    private fun keywordContains(keyword: String?): BooleanExpression? {
        return keyword?.let { product.name.containsIgnoreCase(it) }
    }
}
