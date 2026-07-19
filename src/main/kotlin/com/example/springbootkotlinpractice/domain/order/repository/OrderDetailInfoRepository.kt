package com.example.springbootkotlinpractice.domain.order.repository

import com.example.springbootkotlinpractice.domain.order.entity.OrderDetailInfo
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface OrderDetailInfoRepository : JpaRepository<OrderDetailInfo, Long> {

    fun findByOrderId(orderId: Long): List<OrderDetailInfo>
}
