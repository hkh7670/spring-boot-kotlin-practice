package com.example.springbootkotlinpractice.common.security

data class UserPrincipal(
    val id: Long,
    val role: String? = null,
)
