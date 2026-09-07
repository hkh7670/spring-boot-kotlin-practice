package com.example.springbootkotlinpractice.enums

enum class MemberCouponStatus(
    val desc: String,
) {
    UNUSED("미사용"),
    USED("사용됨"),
    EXPIRED("만료됨"),

}
