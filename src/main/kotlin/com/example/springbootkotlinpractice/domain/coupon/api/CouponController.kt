package com.example.springbootkotlinpractice.domain.coupon.api

import com.example.springbootkotlinpractice.common.dto.CommonResponse
import com.example.springbootkotlinpractice.common.dto.ResponseHandler
import com.example.springbootkotlinpractice.common.security.UserPrincipal
import com.example.springbootkotlinpractice.domain.coupon.dto.MemberCouponResponse
import com.example.springbootkotlinpractice.domain.coupon.service.CouponService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "[COUPON] Coupon", description = "회원 보유 쿠폰 조회 API")
@RequestMapping("/api/v1/coupons")
@RestController
class CouponController(
    private val couponService: CouponService,
) {

    @Operation(
        summary = "사용 가능한 보유 쿠폰 목록 조회 API",
        description = "본인이 보유한 미사용·미만료 쿠폰 목록을 조회한다.",
    )
    @PreAuthorize("hasRole('USER')")
    @GetMapping
    fun getUsableCoupons(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
    ): ResponseEntity<CommonResponse<List<MemberCouponResponse>>> {
        return ResponseHandler.ok(
            couponService.getUsableCoupons(userPrincipal.id)
        )
    }
}
