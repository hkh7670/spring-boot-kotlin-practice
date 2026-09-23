package com.example.springbootkotlinpractice.domain.product.service

import com.example.springbootkotlinpractice.domain.category.repository.CategoryRepository
import com.example.springbootkotlinpractice.domain.product.dto.AdminProductCreateRequest
import com.example.springbootkotlinpractice.domain.product.dto.AdminProductOptionRequest
import com.example.springbootkotlinpractice.domain.product.dto.AdminProductOptionUpdateRequest
import com.example.springbootkotlinpractice.domain.product.dto.AdminProductUpdateRequest
import com.example.springbootkotlinpractice.domain.product.entity.Product
import com.example.springbootkotlinpractice.domain.product.entity.ProductOption
import com.example.springbootkotlinpractice.domain.product.repository.ProductOptionRepository
import com.example.springbootkotlinpractice.domain.product.repository.ProductRepository
import com.example.springbootkotlinpractice.domain.vendor.repository.VendorRepository
import com.example.springbootkotlinpractice.enums.ResponseCodeEnum
import com.example.springbootkotlinpractice.exception.ApiErrorException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

// 관리자 상품 변경의 DB 쓰기 전용 서비스. 검색 색인 같은 외부 I/O는 트랜잭션 밖에서 처리하도록
// AdminProductService가 이 서비스를 호출한 뒤 이어서 수행한다(PaymentService/PaymentRecordService 분리와 동일).
@Service
class AdminProductRecordService(
    private val productRepository: ProductRepository,
    private val productOptionRepository: ProductOptionRepository,
    private val categoryRepository: CategoryRepository,
    private val vendorRepository: VendorRepository,
) {

    @Transactional
    fun createProduct(request: AdminProductCreateRequest): Product {
        validateCategoryAndVendor(request.categoryId, request.vendorId)
        validateOptionNamesUnique(request.productOptions.map { it.name })

        val product = productRepository.save(
            Product.of(
                name = request.name.trim(),
                description = request.description,
                imageUrl = request.imageUrl,
                categoryId = request.categoryId,
                vendorId = request.vendorId,
            )
        )
        saveOptionsOrThrowDuplicate {
            productOptionRepository.saveAll(
                request.productOptions.map {
                    ProductOption.of(
                        product = product,
                        name = it.name.trim(),
                        price = it.price,
                        stockCount = it.stockCount,
                    )
                }
            )
        }
        return product
    }

    @Transactional
    fun updateProduct(productId: Long, request: AdminProductUpdateRequest): Product {
        val product = getActiveProduct(productId)
        validateCategoryAndVendor(request.categoryId, request.vendorId)

        product.update(
            name = request.name.trim(),
            description = request.description,
            imageUrl = request.imageUrl,
            categoryId = request.categoryId,
            vendorId = request.vendorId,
        )
        return product
    }

    @Transactional
    fun deleteProduct(productId: Long) {
        getActiveProduct(productId).delete()
    }

    @Transactional
    fun addOption(productId: Long, request: AdminProductOptionRequest) {
        val product = getActiveProduct(productId)
        val name = request.name.trim()
        if (productOptionRepository.existsByProductIdAndName(productId, name)) {
            throw ApiErrorException(ResponseCodeEnum.DUPLICATED_PRODUCT_OPTION_NAME)
        }

        saveOptionsOrThrowDuplicate {
            productOptionRepository.save(
                ProductOption.of(
                    product = product,
                    name = name,
                    price = request.price,
                    stockCount = request.stockCount,
                )
            )
        }
    }

    @Transactional
    fun updateOption(productId: Long, optionId: Long, request: AdminProductOptionUpdateRequest) {
        getActiveProduct(productId)
        val option = productOptionRepository.findByIdAndProductId(optionId, productId)
            ?: throw ApiErrorException(ResponseCodeEnum.NOT_FOUND_PRODUCT_OPTION)
        val name = request.name.trim()
        if (productOptionRepository.existsByProductIdAndNameAndIdNot(productId, name, optionId)) {
            throw ApiErrorException(ResponseCodeEnum.DUPLICATED_PRODUCT_OPTION_NAME)
        }

        option.update(name = name, price = request.price, stockCount = request.stockCount)
        saveOptionsOrThrowDuplicate { productOptionRepository.flush() }
    }

    private fun getActiveProduct(productId: Long): Product {
        return productRepository.findByIdAndIsDeletedFalse(productId)
            ?: throw ApiErrorException(ResponseCodeEnum.NOT_FOUND_PRODUCT)
    }

    private fun validateCategoryAndVendor(categoryId: Long?, vendorId: Long?) {
        if (categoryId != null && !categoryRepository.existsById(categoryId)) {
            throw ApiErrorException(ResponseCodeEnum.NOT_FOUND_CATEGORY)
        }
        if (vendorId != null && !vendorRepository.existsById(vendorId)) {
            throw ApiErrorException(ResponseCodeEnum.NOT_FOUND_VENDOR)
        }
    }

    // MySQL 기본 콜레이션은 대소문자를 구분하지 않으므로 요청 내 중복 검사도 같은 기준으로 맞춘다
    private fun validateOptionNamesUnique(names: List<String>) {
        val normalizedNames = names.map { it.trim().lowercase() }
        if (normalizedNames.distinct().size != normalizedNames.size) {
            throw ApiErrorException(ResponseCodeEnum.DUPLICATED_PRODUCT_OPTION_NAME)
        }
    }

    // 사전 검증(exists)과 저장 사이에 다른 요청이 같은 옵션명을 넣는 경쟁 상황이나, 대소문자만 다른 이름처럼
    // 사전 검증을 통과했지만 DB 유니크 제약(uq_product_options_01)에 걸리는 경우를 500이 아닌 409로 응답한다
    private fun saveOptionsOrThrowDuplicate(block: () -> Unit) {
        try {
            block()
        } catch (e: DataIntegrityViolationException) {
            throw ApiErrorException(ResponseCodeEnum.DUPLICATED_PRODUCT_OPTION_NAME)
        }
    }
}
