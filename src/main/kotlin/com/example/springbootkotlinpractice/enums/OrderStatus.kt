package com.example.springbootkotlinpractice.enums

enum class OrderStatus(
    val desc: String,
) {
    PENDING_PAYMENT("결제 대기"),
    PAID("결제 완료"),
    SHIPPING("배송 중"),
    DELIVERED("배송 완료"),
    CANCELLED("취소됨"),
    RETURNING("반품 중"),
    RETURNED("반품 완료"),

}
