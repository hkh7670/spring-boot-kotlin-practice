package com.example.springbootkotlinpractice.member.dto

data class MemberCreateRequest(
    val firstName: String,
    val lastName: String,
    val age: Int,
    val phoneNumber: String,
    val email: String,
) {
}