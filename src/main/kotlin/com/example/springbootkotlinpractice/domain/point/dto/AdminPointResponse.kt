package com.example.springbootkotlinpractice.domain.point.dto

import com.example.springbootkotlinpractice.domain.point.entity.Point

data class AdminPointResponse(
    val id: Long,
    val name: String,
    val validDays: Int?,
) {
    companion object {
        fun from(point: Point): AdminPointResponse {
            return AdminPointResponse(
                id = point.id,
                name = point.name,
                validDays = point.validDays,
            )
        }
    }
}

data class PointGrantResponse(
    val grantedCount: Int,
)
