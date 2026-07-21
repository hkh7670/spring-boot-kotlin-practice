package com.example.springbootkotlinpractice.domain.payment.repository

import com.example.springbootkotlinpractice.domain.payment.entity.Payment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PaymentRepository : JpaRepository<Payment, Long> {

    fun existsByOrderId(orderId: Long): Boolean
}
