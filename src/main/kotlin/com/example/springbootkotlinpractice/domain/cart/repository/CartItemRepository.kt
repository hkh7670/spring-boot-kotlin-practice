package com.example.springbootkotlinpractice.domain.cart.repository

import com.example.springbootkotlinpractice.domain.cart.entity.CartItem
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CartItemRepository : JpaRepository<CartItem, Long> {

    fun findByMemberId(memberId: Long): List<CartItem>

    fun findByMemberIdAndProductOptionId(memberId: Long, productOptionId: Long): CartItem?

    fun deleteByMemberIdAndProductOptionId(memberId: Long, productOptionId: Long)
}
