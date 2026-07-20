package com.example.springbootkotlinpractice.domain.order.repository

import com.example.springbootkotlinpractice.domain.order.entity.OrderDetailInfo
import com.example.springbootkotlinpractice.domain.order.entity.OrderInfo
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface OrderDetailInfoRepository : JpaRepository<OrderDetailInfo, Long> {

    fun findByOrderInfoId(orderId: Long): List<OrderDetailInfo>
    fun findByOrderInfo(orderInfo: OrderInfo): List<OrderDetailInfo>
}
