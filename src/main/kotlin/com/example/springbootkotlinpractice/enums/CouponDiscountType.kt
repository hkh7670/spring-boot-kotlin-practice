package com.example.springbootkotlinpractice.enums

enum class CouponDiscountType(
    val desc: String,
) {
    FIXED("정액 할인"),
    PERCENTAGE("정률 할인"),

}
