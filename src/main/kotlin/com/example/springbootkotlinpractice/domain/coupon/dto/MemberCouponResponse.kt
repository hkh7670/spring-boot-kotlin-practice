package com.example.springbootkotlinpractice.domain.coupon.dto

import com.example.springbootkotlinpractice.enums.CouponDiscountType
import java.time.LocalDateTime

data class MemberCouponResponse(
    val memberCouponId: Long,
    val couponName: String,
    val discountType: CouponDiscountType,
    val discountValue: Int,
    val maxDiscountPrice: Int?,
    val minOrderPrice: Int,
    val expiredAt: LocalDateTime,
)
