package com.example.springbootkotlinpractice.enums

enum class OrderStatus(
    val desc: String,
) {
    PENDING_PAYMENT("결제 대기"),
    PAID("결제 완료"),
    CANCELLED("취소됨"),

}
