package com.example.springbootkotlinpractice.domain.point.repository

import com.example.springbootkotlinpractice.domain.point.entity.MemberPointUsage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MemberPointUsageRepository : JpaRepository<MemberPointUsage, Long> {

    fun findByOrderId(orderId: Long): List<MemberPointUsage>
}
