package com.example.springbootkotlinpractice.domain.coupon.dto

import com.example.springbootkotlinpractice.domain.coupon.entity.Coupon
import com.example.springbootkotlinpractice.enums.CouponDiscountType
import java.time.LocalDateTime

data class AdminCouponResponse(
    val id: Long,
    val name: String,
    val discountType: CouponDiscountType,
    val discountValue: Int,
    val maxDiscountPrice: Int?,
    val minOrderPrice: Int,
    val validUntil: LocalDateTime,
) {
    companion object {
        fun from(coupon: Coupon): AdminCouponResponse {
            return AdminCouponResponse(
                id = coupon.id,
                name = coupon.name,
                discountType = coupon.discountType,
                discountValue = coupon.discountValue,
                maxDiscountPrice = coupon.maxDiscountPrice,
                minOrderPrice = coupon.minOrderPrice,
                validUntil = coupon.validUntil,
            )
        }
    }
}

data class CouponIssueResponse(
    val issuedCount: Int,
)
