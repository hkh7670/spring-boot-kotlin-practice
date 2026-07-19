package com.example.springbootkotlinpractice.domain.member.dto

import com.example.springbootkotlinpractice.enums.JoinProvider
import java.time.LocalDate

data class MemberResponse(
    val id: Long,
    val uuid: String,
    val firstName: String,
    val lastName: String,
    val birthDate: LocalDate?,
    val phoneNumber: String,
    val email: String?,
    val joinProvider: JoinProvider,
) {
}