package com.example.springbootkotlinpractice.domain.payment.repository

import com.example.springbootkotlinpractice.domain.payment.entity.PayInfo
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PayInfoRepository : JpaRepository<PayInfo, Long> {

    fun existsByOrderId(orderId: Long): Boolean
}
