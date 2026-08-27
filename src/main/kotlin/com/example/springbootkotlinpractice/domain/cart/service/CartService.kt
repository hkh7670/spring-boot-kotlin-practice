package com.example.springbootkotlinpractice.domain.cart.service

import com.example.springbootkotlinpractice.domain.cart.dto.CartItemAddRequest
import com.example.springbootkotlinpractice.domain.cart.dto.CartItemCountRequest
import com.example.springbootkotlinpractice.domain.cart.dto.CartItemResponse
import com.example.springbootkotlinpractice.domain.cart.dto.CartResponse
import com.example.springbootkotlinpractice.domain.cart.entity.CartItem
import com.example.springbootkotlinpractice.domain.cart.repository.CartItemRepository
import com.example.springbootkotlinpractice.domain.product.entity.ProductOption
import com.example.springbootkotlinpractice.domain.product.repository.ProductOptionRepository
import com.example.springbootkotlinpractice.enums.ResponseCodeEnum
import com.example.springbootkotlinpractice.exception.ApiErrorException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.CollectionUtils

@Service
class CartService(
    private val cartItemRepository: CartItemRepository,
    private val productOptionRepository: ProductOptionRepository,
) {

    @Transactional
    fun addItem(memberId: Long, request: CartItemAddRequest): CartResponse {
        val productOption = getProductOption(request.productOptionId)
        val existing = cartItemRepository.findByMemberIdAndProductOptionId(memberId, request.productOptionId)
        val newCount = (existing?.count ?: 0) + request.count
        validateStock(productOption, newCount)

        if (existing != null) {
            existing.count = newCount
        } else {
            cartItemRepository.save(CartItem.of(memberId, request.productOptionId, newCount))
        }

        return getCart(memberId)
    }

    @Transactional
    fun updateCount(memberId: Long, productOptionId: Long, request: CartItemCountRequest): CartResponse {
        val cartItem = cartItemRepository.findByMemberIdAndProductOptionId(memberId, productOptionId)
            ?: throw ApiErrorException(ResponseCodeEnum.NOT_FOUND_CART_ITEM)

        validateStock(getProductOption(productOptionId), request.count)
        cartItem.count = request.count

        return getCart(memberId)
    }

    @Transactional
    fun removeItem(memberId: Long, productOptionId: Long): CartResponse {
        cartItemRepository.deleteByMemberIdAndProductOptionId(memberId, productOptionId)
        return getCart(memberId)
    }

    @Transactional(readOnly = true)
    fun getCart(memberId: Long): CartResponse {
        val cartItems = cartItemRepository.findByMemberId(memberId)
        if (CollectionUtils.isEmpty(cartItems)) {
            return CartResponse(items = emptyList())
        }

        val productOptionMap = productOptionRepository
            .findByIdInFetchProduct(cartItems.map { it.productOptionId })
            .associateBy { it.id }

        val items = cartItems.mapNotNull { cartItem ->
            val productOption = productOptionMap[cartItem.productOptionId] ?: return@mapNotNull null
            CartItemResponse(
                productOptionId = productOption.id,
                productId = productOption.product.id,
                productName = productOption.product.name,
                optionName = productOption.name,
                price = productOption.product.price,
                imageUrl = productOption.product.imageUrl,
                count = cartItem.count,
                soldOut = productOption.stockCount <= 0,
            )
        }

        return CartResponse(items = items)
    }

    private fun validateStock(productOption: ProductOption, requestedCount: Int) {
        if (requestedCount > productOption.stockCount) {
            throw ApiErrorException(ResponseCodeEnum.NOT_ENOUGH_STOCK)
        }
    }

    private fun getProductOption(productOptionId: Long): ProductOption {
        return productOptionRepository.findByIdFetchProduct(productOptionId)
            ?: throw ApiErrorException(ResponseCodeEnum.NOT_FOUND_PRODUCT_OPTION)
    }
}
