package com.example.springbootkotlinpractice.domain.order.repository

import com.example.springbootkotlinpractice.domain.order.entity.OrderInfo
import com.example.springbootkotlinpractice.enums.OrderStatus
import java.time.LocalDateTime
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface OrderInfoRepository : JpaRepository<OrderInfo, Long> {

    fun findByOrderUid(orderUid: String): OrderInfo?

    fun findByIdAndMemberId(id: Long, memberId: Long): OrderInfo?

    fun findByStatusAndCreatedDatetimeBefore(status: OrderStatus, createdDatetime: LocalDateTime): List<OrderInfo>
}
