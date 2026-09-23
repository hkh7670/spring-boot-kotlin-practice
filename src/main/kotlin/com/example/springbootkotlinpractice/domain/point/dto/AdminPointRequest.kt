package com.example.springbootkotlinpractice.domain.point.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

@Schema(description = "포인트 템플릿 생성/수정 요청. 수정 시에는 요청 값으로 전체를 교체하며 validDays를 null로 보내면 무제한이 된다.")
data class AdminPointRequest(
    @field:Schema(description = "포인트 지급 사유/종류 명", example = "이벤트 지급")
    @field:NotBlank
    @field:Size(max = 100)
    val name: String,

    @field:Schema(description = "적립일로부터 유효 일수 (null이면 만료 없음)", example = "365")
    @field:Positive
    @field:Max(3650)
    val validDays: Int? = null,
)

data class PointGrantRequest(
    @field:Schema(description = "포인트를 적립할 회원 ID 목록 (1~100명)", example = "[1, 2, 3]")
    @field:NotEmpty
    @field:Size(max = 100)
    val memberIds: List<Long>,

    @field:Schema(description = "회원당 적립 금액", example = "5000")
    @field:Positive
    @field:Max(10_000_000)
    val amount: Int,
)
