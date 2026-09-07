package com.example.springbootkotlinpractice.domain.coupon.entity

import com.example.springbootkotlinpractice.common.entity.BaseTimeEntity
import com.example.springbootkotlinpractice.enums.CouponDiscountType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "coupons", comment = "쿠폰 템플릿(정의) 정보")
class Coupon(

    @Column(name = "name", nullable = false, length = 100, comment = "쿠폰 명")
    var name: String,

    @Enumerated(EnumType.STRING)
    @Column(
        name = "discount_type", nullable = false, length = 20,
        comment = "할인 방식 (FIXED/PERCENTAGE)",
    )
    val discountType: CouponDiscountType,

    @Column(name = "discount_value", nullable = false, comment = "할인액(FIXED) 또는 할인율%(PERCENTAGE)")
    var discountValue: Int,

    @Column(
        name = "max_discount_price", nullable = true,
        comment = "정률 할인 시 최대 할인 금액 (FIXED이면 NULL)",
    )
    var maxDiscountPrice: Int? = null,

    @Column(name = "min_order_price", nullable = false, comment = "쿠폰 적용 가능한 최소 주문 금액(상품 금액 기준)")
    var minOrderPrice: Int = 0,

    @Column(name = "valid_until", nullable = false, comment = "쿠폰 사용 가능 마감 일시 (캠페인 공통)")
    val validUntil: LocalDateTime,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0L

    // 정액/정률 방식에 맞춰 실제 할인 금액을 계산한다. 정률이면 maxDiscountPrice로 상한을 건다
    fun calculateDiscountPrice(productTotalPrice: Int): Int {
        val rawDiscount = when (discountType) {
            CouponDiscountType.FIXED -> discountValue
            CouponDiscountType.PERCENTAGE -> productTotalPrice * discountValue / 100
        }
        val cappedDiscount = maxDiscountPrice?.let { minOf(rawDiscount, it) } ?: rawDiscount
        return minOf(cappedDiscount, productTotalPrice)
    }

    companion object {
        fun of(
            name: String,
            discountType: CouponDiscountType,
            discountValue: Int,
            maxDiscountPrice: Int? = null,
            minOrderPrice: Int = 0,
            validUntil: LocalDateTime,
        ): Coupon {
            return Coupon(
                name = name,
                discountType = discountType,
                discountValue = discountValue,
                maxDiscountPrice = maxDiscountPrice,
                minOrderPrice = minOrderPrice,
                validUntil = validUntil,
            )
        }
    }
}
