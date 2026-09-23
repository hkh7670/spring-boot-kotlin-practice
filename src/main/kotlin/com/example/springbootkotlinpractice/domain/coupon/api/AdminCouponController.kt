package com.example.springbootkotlinpractice.domain.coupon.api

import com.example.springbootkotlinpractice.common.dto.CommonResponse
import com.example.springbootkotlinpractice.common.dto.PageResponse
import com.example.springbootkotlinpractice.common.dto.ResponseHandler
import com.example.springbootkotlinpractice.common.security.UserPrincipal
import com.example.springbootkotlinpractice.domain.coupon.dto.AdminCouponCreateRequest
import com.example.springbootkotlinpractice.domain.coupon.dto.AdminCouponResponse
import com.example.springbootkotlinpractice.domain.coupon.dto.AdminCouponUpdateRequest
import com.example.springbootkotlinpractice.domain.coupon.dto.CouponIssueRequest
import com.example.springbootkotlinpractice.domain.coupon.dto.CouponIssueResponse
import com.example.springbootkotlinpractice.domain.coupon.service.AdminCouponService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "[ADMIN] Coupon", description = "관리자 쿠폰 템플릿 관리 / 회원 발급 API")
@RequestMapping("/api/v1/admin/coupons")
@RestController
class AdminCouponController(
    private val adminCouponService: AdminCouponService,
) {

    @Operation(
        summary = "쿠폰 템플릿 생성 API",
        description = "정액(FIXED) 또는 정률(PERCENTAGE) 쿠폰 템플릿을 생성한다.",
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    fun createCoupon(
        @Valid @RequestBody request: AdminCouponCreateRequest,
    ): ResponseEntity<CommonResponse<AdminCouponResponse>> {
        return ResponseHandler.created(adminCouponService.createCoupon(request))
    }

    @Operation(
        summary = "쿠폰 템플릿 목록 조회 API",
        description = "삭제되지 않은 쿠폰 템플릿을 최신순으로 페이징 조회한다.",
    )
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    fun getCoupons(
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
    ): ResponseEntity<CommonResponse<PageResponse<AdminCouponResponse>>> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"))
        return ResponseHandler.ok(adminCouponService.getCoupons(pageable))
    }

    @Operation(summary = "쿠폰 템플릿 단건 조회 API")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{couponId}")
    fun getCoupon(@PathVariable couponId: Long): ResponseEntity<CommonResponse<AdminCouponResponse>> {
        return ResponseHandler.ok(adminCouponService.getCoupon(couponId))
    }

    @Operation(
        summary = "쿠폰 템플릿 수정 API",
        description = "이름, 할인값, 최대 할인 금액, 최소 주문 금액을 요청 값으로 교체한다. " +
                "발급된 쿠폰이 있으면 이름 외의 조건을 변경할 수 없다(409).",
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{couponId}")
    fun updateCoupon(
        @PathVariable couponId: Long,
        @Valid @RequestBody request: AdminCouponUpdateRequest,
    ): ResponseEntity<CommonResponse<AdminCouponResponse>> {
        return ResponseHandler.ok(adminCouponService.updateCoupon(couponId, request))
    }

    @Operation(
        summary = "쿠폰 템플릿 삭제 API",
        description = "쿠폰 템플릿을 soft delete 처리해 신규 발급을 중단한다. 이미 발급된 쿠폰은 만료일까지 사용할 수 있다.",
    )
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{couponId}")
    fun deleteCoupon(@PathVariable couponId: Long): ResponseEntity<CommonResponse<Unit>> {
        adminCouponService.deleteCoupon(couponId)
        return ResponseHandler.ok()
    }

    @Operation(
        summary = "쿠폰 회원 지정 발급 API",
        description = "지정한 회원들에게 쿠폰을 발급한다. 존재하지 않는 회원이나 이미 발급받은 회원이 하나라도 있으면 " +
                "전체 요청이 실패하고 아무에게도 발급되지 않는다.",
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{couponId}/issue")
    fun issueCoupon(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable couponId: Long,
        @Valid @RequestBody request: CouponIssueRequest,
    ): ResponseEntity<CommonResponse<CouponIssueResponse>> {
        return ResponseHandler.created(
            adminCouponService.issueCoupon(userPrincipal.id, couponId, request)
        )
    }
}
