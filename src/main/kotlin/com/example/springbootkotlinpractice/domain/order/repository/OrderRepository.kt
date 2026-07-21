package com.example.springbootkotlinpractice.domain.order.repository

import com.example.springbootkotlinpractice.domain.order.entity.Order
import com.example.springbootkotlinpractice.enums.OrderStatus
import java.time.LocalDateTime
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface OrderRepository : JpaRepository<Order, Long> {

    fun findByOrderUid(orderUid: String): Order?

    fun findByIdAndMemberId(id: Long, memberId: Long): Order?

    fun findByStatusAndCreatedDatetimeBefore(status: OrderStatus, createdDatetime: LocalDateTime): List<Order>
}
