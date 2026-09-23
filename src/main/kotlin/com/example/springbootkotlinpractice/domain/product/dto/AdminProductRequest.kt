package com.example.springbootkotlinpractice.domain.product.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size

@Schema(description = "상품 옵션 생성/추가 요청")
data class AdminProductOptionRequest(
    @field:Schema(description = "옵션 명 (상품 내 중복 불가, 대소문자 무시)", example = "블랙 / L")
    @field:NotBlank
    @field:Size(max = 100)
    val name: String,

    @field:Schema(description = "옵션 가격 (최대 1억)", example = "10000")
    @field:Positive
    @field:Max(100_000_000)
    val price: Int,

    @field:Schema(description = "옵션 재고 수량 (최대 100만)", example = "10")
    @field:PositiveOrZero
    @field:Max(1_000_000)
    val stockCount: Int,
)

@Schema(
    description = "상품 옵션 수정 요청. 이름과 가격은 요청 값으로 교체한다. " +
            "재고는 null이면 변경하지 않는다(주문으로 줄어든 재고를 오래된 값으로 덮어쓰지 않도록). " +
            "값을 보내면 그 값으로 덮어쓴다."
)
data class AdminProductOptionUpdateRequest(
    @field:Schema(description = "옵션 명 (상품 내 중복 불가, 대소문자 무시)", example = "블랙 / L")
    @field:NotBlank
    @field:Size(max = 100)
    val name: String,

    @field:Schema(description = "옵션 가격 (최대 1억)", example = "10000")
    @field:Positive
    @field:Max(100_000_000)
    val price: Int,

    @field:Schema(description = "옵션 재고 수량 (최대 100만, null이면 변경하지 않음)", example = "10")
    @field:PositiveOrZero
    @field:Max(1_000_000)
    val stockCount: Int? = null,
)

data class AdminProductCreateRequest(
    @field:Schema(description = "상품 명", example = "기능성 러닝 티셔츠")
    @field:NotBlank
    @field:Size(max = 50)
    val name: String,

    @field:Schema(description = "상품 상세 설명 (최대 10,000자)")
    @field:Size(max = 10_000)
    val description: String? = null,

    @field:Schema(description = "대표 이미지 URL", example = "https://example.com/image.png")
    @field:Size(max = 500)
    val imageUrl: String? = null,

    @field:Schema(description = "카테고리 ID", example = "1")
    val categoryId: Long? = null,

    @field:Schema(description = "업체 ID", example = "1")
    val vendorId: Long? = null,

    @field:Schema(description = "상품 옵션 목록 (최소 1개, 옵션이 없는 단순 상품도 기본 옵션 1개를 등록한다)")
    @field:Valid
    @field:NotEmpty
    @field:Size(max = 50)
    val productOptions: List<AdminProductOptionRequest>,
)

@Schema(description = "상품 기본정보 수정 요청. 수정 가능한 필드 전체를 교체하며, null로 보내면 해당 값이 비워진다.")
data class AdminProductUpdateRequest(
    @field:Schema(description = "상품 명", example = "기능성 러닝 티셔츠")
    @field:NotBlank
    @field:Size(max = 50)
    val name: String,

    @field:Schema(description = "상품 상세 설명 (최대 10,000자)")
    @field:Size(max = 10_000)
    val description: String? = null,

    @field:Schema(description = "대표 이미지 URL", example = "https://example.com/image.png")
    @field:Size(max = 500)
    val imageUrl: String? = null,

    @field:Schema(description = "카테고리 ID", example = "1")
    val categoryId: Long? = null,

    @field:Schema(description = "업체 ID", example = "1")
    val vendorId: Long? = null,
)
