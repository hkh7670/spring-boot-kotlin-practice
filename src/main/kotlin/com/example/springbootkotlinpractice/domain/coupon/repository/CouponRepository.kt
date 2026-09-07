package com.example.springbootkotlinpractice.domain.coupon.repository

import com.example.springbootkotlinpractice.domain.coupon.entity.Coupon
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CouponRepository : JpaRepository<Coupon, Long>
