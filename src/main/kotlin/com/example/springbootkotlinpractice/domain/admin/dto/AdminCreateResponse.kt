package com.example.springbootkotlinpractice.domain.admin.dto

import com.example.springbootkotlinpractice.domain.admin.entity.Admin
import com.example.springbootkotlinpractice.enums.AdminStatus
import io.swagger.v3.oas.annotations.media.Schema

data class AdminCreateResponse(
    @field:Schema(description = "관리자 고유 식별자")
    val id: Long,

    @field:Schema(description = "이름")
    val name: String,

    @field:Schema(description = "이메일 주소")
    val email: String,

    @field:Schema(description = "계정 상태")
    val status: AdminStatus,
) {
    companion object {
        fun from(admin: Admin): AdminCreateResponse {
            return AdminCreateResponse(
                id = admin.id,
                name = admin.name,
                email = admin.email,
                status = admin.status,
            )
        }
    }
}
