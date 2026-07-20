package com.example.springbootkotlinpractice.domain.order.repository

import com.example.springbootkotlinpractice.domain.order.entity.OrderStatusHistory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface OrderStatusHistoryRepository : JpaRepository<OrderStatusHistory, Long> {

    fun findByOrderIdOrderByCreatedDatetimeAsc(orderId: Long): List<OrderStatusHistory>
}
