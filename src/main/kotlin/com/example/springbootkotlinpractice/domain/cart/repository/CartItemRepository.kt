package com.example.springbootkotlinpractice.domain.cart.repository

import com.example.springbootkotlinpractice.domain.cart.entity.CartItem
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CartItemRepository : JpaRepository<CartItem, Long> {

    fun findByMemberId(memberId: Long): List<CartItem>

    fun findByMemberIdAndProductId(memberId: Long, productId: Long): CartItem?

    fun deleteByMemberIdAndProductId(memberId: Long, productId: Long)
}
