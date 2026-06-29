package com.example.springbootkotlinpractice.enums

enum class JoinProvider(
    val desc: String,
) {
    EMAIL("이메일"),
    GOOGLE("구글"),
    NAVER("네이버"),
    KAKAO("카카오"),

}