package com.example.springbootkotlinpractice.enums

enum class MemberPointStatus(
    val desc: String,
) {
    ACTIVE("사용 가능"),
    EXHAUSTED("전액 소진"),
    EXPIRED("만료됨"),

}
