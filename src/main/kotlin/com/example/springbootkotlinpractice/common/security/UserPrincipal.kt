package com.example.springbootkotlinpractice.common.security

import com.example.springbootkotlinpractice.enums.Role

data class UserPrincipal(
    val id: Long,
    val role: Role? = null,
)
