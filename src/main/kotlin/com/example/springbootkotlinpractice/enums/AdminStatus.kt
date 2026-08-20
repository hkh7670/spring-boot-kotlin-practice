package com.example.springbootkotlinpractice.enums

enum class AdminStatus(
    val desc: String,
) {
    ACTIVE("정상"),
    INACTIVE("휴면"),
    WITHDRAWN("탈퇴"),
}
