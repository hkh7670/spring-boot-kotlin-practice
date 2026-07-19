package com.example.springbootkotlinpractice.domain.order.repository

import com.example.springbootkotlinpractice.domain.order.entity.OrderInfo
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface OrderInfoRepository : JpaRepository<OrderInfo, Long> {

    fun findByOrderUid(orderUid: String): OrderInfo?

    fun findByIdAndMemberId(id: Long, memberId: Long): OrderInfo?
}
