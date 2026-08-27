package com.example.springbootkotlinpractice.domain.cart.service

import com.example.springbootkotlinpractice.domain.cart.dto.CartItemAddRequest
import com.example.springbootkotlinpractice.domain.cart.dto.CartItemCountRequest
import com.example.springbootkotlinpractice.domain.cart.dto.CartItemResponse
import com.example.springbootkotlinpractice.domain.cart.dto.CartResponse
import com.example.springbootkotlinpractice.domain.cart.entity.CartItem
import com.example.springbootkotlinpractice.domain.cart.repository.CartItemRepository
import com.example.springbootkotlinpractice.domain.product.entity.Product
import com.example.springbootkotlinpractice.domain.product.repository.ProductRepository
import com.example.springbootkotlinpractice.enums.ResponseCodeEnum
import com.example.springbootkotlinpractice.exception.ApiErrorException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.CollectionUtils

@Service
class CartService(
    private val cartItemRepository: CartItemRepository,
    private val productRepository: ProductRepository,
) {

    @Transactional
    fun addItem(memberId: Long, request: CartItemAddRequest): CartResponse {
        val product = getProduct(request.productId)
        val existing = cartItemRepository.findByMemberIdAndProductId(memberId, request.productId)
        val newCount = (existing?.count ?: 0) + request.count
        validateStock(product, newCount)

        if (existing != null) {
            existing.count = newCount
        } else {
            cartItemRepository.save(CartItem.of(memberId, request.productId, newCount))
        }

        return getCart(memberId)
    }

    @Transactional
    fun updateCount(memberId: Long, productId: Long, request: CartItemCountRequest): CartResponse {
        val cartItem = cartItemRepository.findByMemberIdAndProductId(memberId, productId)
            ?: throw ApiErrorException(ResponseCodeEnum.NOT_FOUND_CART_ITEM)

        validateStock(getProduct(productId), request.count)
        cartItem.count = request.count

        return getCart(memberId)
    }

    @Transactional
    fun removeItem(memberId: Long, productId: Long): CartResponse {
        cartItemRepository.deleteByMemberIdAndProductId(memberId, productId)
        return getCart(memberId)
    }

    @Transactional(readOnly = true)
    fun getCart(memberId: Long): CartResponse {
        val cartItems = cartItemRepository.findByMemberId(memberId)
        if (CollectionUtils.isEmpty(cartItems)) {
            return CartResponse(items = emptyList())
        }

        val productMap = productRepository.findAllById(cartItems.map { it.productId }).associateBy { it.id }

        val items = cartItems.mapNotNull { cartItem ->
            val product = productMap[cartItem.productId] ?: return@mapNotNull null
            CartItemResponse(
                productId = product.id,
                productName = product.name,
                price = product.price,
                imageUrl = product.imageUrl,
                count = cartItem.count,
                soldOut = product.stockCount <= 0,
            )
        }

        return CartResponse(items = items)
    }

    private fun validateStock(product: Product, requestedCount: Int) {
        if (requestedCount > product.stockCount) {
            throw ApiErrorException(ResponseCodeEnum.NOT_ENOUGH_STOCK)
        }
    }

    private fun getProduct(productId: Long): Product {
        return productRepository.findByIdOrNull(productId)
            ?: throw ApiErrorException(ResponseCodeEnum.NOT_FOUND_PRODUCT)
    }
}
