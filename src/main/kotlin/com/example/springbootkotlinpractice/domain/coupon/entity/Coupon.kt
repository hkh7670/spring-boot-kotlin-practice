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
import org.hibernate.annotations.Comment

@Entity
@Table(name = "coupons")
@Comment("쿠폰 템플릿(정의) 정보")
class Coupon(

    @Comment("쿠폰 명")
    @Column(name = "name", nullable = false, length = 100)
    var name: String,

    @Comment("할인 방식 (FIXED/PERCENTAGE)")
    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    val discountType: CouponDiscountType,

    @Comment("할인액(FIXED) 또는 할인율%(PERCENTAGE)")
    @Column(name = "discount_value", nullable = false)
    var discountValue: Int,

    @Comment("정률 할인 시 최대 할인 금액 (FIXED이면 NULL)")
    @Column(name = "max_discount_price", nullable = true)
    var maxDiscountPrice: Int? = null,

    @Comment("쿠폰 적용 가능한 최소 주문 금액(상품 금액 기준)")
    @Column(name = "min_order_price", nullable = false)
    var minOrderPrice: Int = 0,

    @Comment("쿠폰 사용 가능 마감 일시 (캠페인 공통)")
    @Column(name = "valid_until", nullable = false)
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
