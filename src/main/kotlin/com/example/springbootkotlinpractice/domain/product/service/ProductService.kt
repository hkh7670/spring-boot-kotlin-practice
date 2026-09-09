package com.example.springbootkotlinpractice.domain.product.service

import com.example.springbootkotlinpractice.common.dto.PageResponse
import com.example.springbootkotlinpractice.domain.category.repository.CategoryRepository
import com.example.springbootkotlinpractice.domain.product.dto.ProductDetailResponse
import com.example.springbootkotlinpractice.domain.product.dto.ProductOptionResponse
import com.example.springbootkotlinpractice.domain.product.dto.ProductSummaryResponse
import com.example.springbootkotlinpractice.domain.product.entity.Product
import com.example.springbootkotlinpractice.domain.product.repository.ProductOptionAggregate
import com.example.springbootkotlinpractice.domain.product.repository.ProductOptionRepository
import com.example.springbootkotlinpractice.domain.product.repository.ProductRepository
import com.example.springbootkotlinpractice.domain.vendor.repository.VendorRepository
import com.example.springbootkotlinpractice.enums.ResponseCodeEnum
import com.example.springbootkotlinpractice.exception.ApiErrorException
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProductService(
    private val productRepository: ProductRepository,
    private val productOptionRepository: ProductOptionRepository,
    private val categoryRepository: CategoryRepository,
    private val vendorRepository: VendorRepository,
) {

    @Transactional(readOnly = true)
    fun getProducts(categoryId: Long?, keyword: String?, pageable: Pageable): PageResponse<ProductSummaryResponse> {
        val products = productRepository.search(categoryId, keyword, pageable)
        val vendorNameMap = findVendorNameMap(products.content)
        val aggregateMap = findAggregateMap(products.content)

        return PageResponse.of(
            products.map {
                val aggregate = aggregateMap[it.id]
                ProductSummaryResponse(
                    id = it.id,
                    name = it.name,
                    price = aggregate?.getMinPrice()?.toInt() ?: 0,
                    imageUrl = it.imageUrl,
                    stockCount = aggregate?.getTotalStock()?.toInt() ?: 0,
                    categoryId = it.categoryId,
                    vendorName = vendorNameMap[it.vendorId],
                )
            }
        )
    }

    @Transactional(readOnly = true)
    fun getProduct(productId: Long): ProductDetailResponse {
        val product = productRepository.findByIdOrNull(productId)
            ?: throw ApiErrorException(ResponseCodeEnum.NOT_FOUND_PRODUCT)

        val categoryName = product.categoryId?.let { categoryRepository.findByIdOrNull(it)?.name }
        val vendorName = product.vendorId?.let { vendorRepository.findByIdOrNull(it)?.name }
        val productOptions = productOptionRepository.findByProductId(productId).map {
            ProductOptionResponse(id = it.id, name = it.name, price = it.price, stockCount = it.stockCount)
        }

        return ProductDetailResponse(
            id = product.id,
            name = product.name,
            description = product.description,
            imageUrl = product.imageUrl,
            productOptions = productOptions,
            categoryId = product.categoryId,
            categoryName = categoryName,
            vendorId = product.vendorId,
            vendorName = vendorName,
        )
    }

    private fun findVendorNameMap(products: List<Product>): Map<Long, String> {
        val vendorIds = products.mapNotNull { it.vendorId }.distinct()
        if (vendorIds.isEmpty()) {
            return emptyMap()
        }
        return vendorRepository.findAllById(vendorIds).associateBy({ it.id }, { it.name })
    }

    private fun findAggregateMap(products: List<Product>): Map<Long, ProductOptionAggregate> {
        if (products.isEmpty()) {
            return emptyMap()
        }
        return productOptionRepository.findAggregatesByProductIdIn(products.map { it.id })
            .associateBy { it.getProductId() }
    }
}
