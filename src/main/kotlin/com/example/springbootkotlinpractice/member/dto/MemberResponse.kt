package com.example.springbootkotlinpractice.member.dto

data class MemberResponse(
    val id: Long,
    val uuid: String,
    val firstName: String,
    val lastName: String,
    val age: Int,
    val phoneNumber: String,
    val email: String,
) {
}