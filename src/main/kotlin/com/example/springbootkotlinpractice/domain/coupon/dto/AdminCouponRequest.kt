package com.example.springbootkotlinpractice.domain.coupon.dto

import com.example.springbootkotlinpractice.enums.CouponDiscountType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Future
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

data class AdminCouponCreateRequest(
    @field:Schema(description = "쿠폰 명", example = "신규가입 5천원 할인")
    @field:NotBlank
    @field:Size(max = 100)
    val name: String,

    @field:Schema(description = "할인 방식 (FIXED: 정액, PERCENTAGE: 정률)", example = "FIXED")
    @field:NotNull
    val discountType: CouponDiscountType,

    @field:Schema(description = "할인액(FIXED, 원) 또는 할인율(PERCENTAGE, 1~100)", example = "5000")
    @field:Positive
    @field:Max(10_000_000)
    val discountValue: Int,

    @field:Schema(description = "정률 할인 시 최대 할인 금액. 정액 쿠폰이면 null이어야 한다", example = "10000")
    @field:Positive
    @field:Max(10_000_000)
    val maxDiscountPrice: Int? = null,

    @field:Schema(description = "쿠폰 적용 가능한 최소 주문 금액(상품 금액 기준)", example = "30000")
    @field:PositiveOrZero
    @field:Max(100_000_000)
    val minOrderPrice: Int = 0,

    @field:Schema(description = "쿠폰 사용 가능 마감 일시 (현재보다 미래)", example = "2030-12-31T23:59:59")
    @field:NotNull
    @field:Future
    val validUntil: LocalDateTime,
)

@Schema(
    description = "쿠폰 수정 요청. 할인 방식과 마감 일시는 수정할 수 없고, " +
            "이미 발급된 쿠폰이 있으면 이름 외의 조건은 변경할 수 없다."
)
data class AdminCouponUpdateRequest(
    @field:Schema(description = "쿠폰 명", example = "신규가입 5천원 할인")
    @field:NotBlank
    @field:Size(max = 100)
    val name: String,

    @field:Schema(description = "할인액(FIXED, 원) 또는 할인율(PERCENTAGE, 1~100)", example = "5000")
    @field:Positive
    @field:Max(10_000_000)
    val discountValue: Int,

    @field:Schema(description = "정률 할인 시 최대 할인 금액. 정액 쿠폰이면 null이어야 한다", example = "10000")
    @field:Positive
    @field:Max(10_000_000)
    val maxDiscountPrice: Int? = null,

    @field:Schema(description = "쿠폰 적용 가능한 최소 주문 금액(상품 금액 기준)", example = "30000")
    @field:PositiveOrZero
    @field:Max(100_000_000)
    val minOrderPrice: Int = 0,
)

data class CouponIssueRequest(
    @field:Schema(description = "쿠폰을 발급할 회원 ID 목록 (1~100명)", example = "[1, 2, 3]")
    @field:NotEmpty
    @field:Size(max = 100)
    val memberIds: List<Long>,
)
