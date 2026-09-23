package com.example.springbootkotlinpractice.domain.coupon.service

import com.example.springbootkotlinpractice.common.Logging
import com.example.springbootkotlinpractice.common.dto.PageResponse
import com.example.springbootkotlinpractice.domain.coupon.dto.AdminCouponCreateRequest
import com.example.springbootkotlinpractice.domain.coupon.dto.AdminCouponResponse
import com.example.springbootkotlinpractice.domain.coupon.dto.AdminCouponUpdateRequest
import com.example.springbootkotlinpractice.domain.coupon.dto.CouponIssueRequest
import com.example.springbootkotlinpractice.domain.coupon.dto.CouponIssueResponse
import com.example.springbootkotlinpractice.domain.coupon.entity.Coupon
import com.example.springbootkotlinpractice.domain.coupon.entity.MemberCoupon
import com.example.springbootkotlinpractice.domain.coupon.repository.CouponRepository
import com.example.springbootkotlinpractice.domain.coupon.repository.MemberCouponRepository
import com.example.springbootkotlinpractice.domain.member.repository.MemberRepository
import com.example.springbootkotlinpractice.enums.CouponDiscountType
import com.example.springbootkotlinpractice.enums.ResponseCodeEnum
import com.example.springbootkotlinpractice.exception.ApiErrorException
import java.time.LocalDateTime
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminCouponService(
    private val couponRepository: CouponRepository,
    private val memberCouponRepository: MemberCouponRepository,
    private val memberRepository: MemberRepository,
) : Logging {

    @Transactional
    fun createCoupon(request: AdminCouponCreateRequest): AdminCouponResponse {
        validateDiscountPolicy(
            request.discountType,
            request.discountValue,
            request.maxDiscountPrice,
        )

        val coupon = couponRepository.save(
            Coupon.of(
                name = request.name,
                discountType = request.discountType,
                discountValue = request.discountValue,
                maxDiscountPrice = request.maxDiscountPrice,
                minOrderPrice = request.minOrderPrice,
                validUntil = request.validUntil,
            )
        )
        return AdminCouponResponse.from(coupon)
    }

    @Transactional(readOnly = true)
    fun getCoupons(pageable: Pageable): PageResponse<AdminCouponResponse> {
        return PageResponse.of(
            couponRepository.findAllByIsDeletedFalse(pageable).map { AdminCouponResponse.from(it) }
        )
    }

    @Transactional(readOnly = true)
    fun getCoupon(couponId: Long): AdminCouponResponse {
        return AdminCouponResponse.from(getActiveCoupon(couponId))
    }

    @Transactional
    fun updateCoupon(couponId: Long, request: AdminCouponUpdateRequest): AdminCouponResponse {
        val coupon = getActiveCoupon(couponId)
        validateDiscountPolicy(coupon.discountType, request.discountValue, request.maxDiscountPrice)

        // 사용 시점에 템플릿을 실시간 조회하므로, 발급된 쿠폰이 있으면 할인 조건 변경이 이미 받은 고객의
        // 혜택을 바꿔버린다. 이름 변경만 허용한다.
        val conditionChanged = coupon.discountValue != request.discountValue ||
                coupon.maxDiscountPrice != request.maxDiscountPrice ||
                coupon.minOrderPrice != request.minOrderPrice
        if (conditionChanged && memberCouponRepository.existsByCouponId(couponId)) {
            throw ApiErrorException(ResponseCodeEnum.COUPON_ALREADY_ISSUED)
        }

        coupon.update(
            name = request.name,
            discountValue = request.discountValue,
            maxDiscountPrice = request.maxDiscountPrice,
            minOrderPrice = request.minOrderPrice,
        )
        return AdminCouponResponse.from(coupon)
    }

    // 삭제는 신규 발급만 중단한다. 이미 발급된 쿠폰은 만료일까지 계속 사용할 수 있다
    @Transactional
    fun deleteCoupon(couponId: Long) {
        getActiveCoupon(couponId).delete()
    }

    @Transactional
    fun issueCoupon(adminId: Long, couponId: Long, request: CouponIssueRequest): CouponIssueResponse {
        val coupon = getActiveCoupon(couponId)
        if (!coupon.validUntil.isAfter(LocalDateTime.now())) {
            throw ApiErrorException(ResponseCodeEnum.EXPIRED_COUPON)
        }

        val memberIds = request.memberIds.distinct()
        if (memberRepository.countByIdIn(memberIds) != memberIds.size.toLong()) {
            throw ApiErrorException(ResponseCodeEnum.NOT_FOUND_USER)
        }
        if (memberCouponRepository.findIssuedMemberIds(couponId, memberIds).isNotEmpty()) {
            throw ApiErrorException(ResponseCodeEnum.DUPLICATED_MEMBER_COUPON)
        }

        // 사전 검증과 저장 사이에 같은 발급 요청이 겹치면 uq_member_coupons_01에 걸리므로 500이 아닌 409로 응답한다
        try {
            memberCouponRepository.saveAll(
                memberIds.map { MemberCoupon.of(it, couponId, coupon.validUntil) }
            )
        } catch (e: DataIntegrityViolationException) {
            throw ApiErrorException(ResponseCodeEnum.DUPLICATED_MEMBER_COUPON)
        }

        // 금전적 혜택이 나가는 작업이라 누가 누구에게 발급했는지 감사 로그를 남긴다
        logger.info("[ADMIN] 쿠폰 발급 adminId={} couponId={} memberIds={}", adminId, couponId, memberIds)
        return CouponIssueResponse(issuedCount = memberIds.size)
    }

    private fun getActiveCoupon(couponId: Long): Coupon {
        return couponRepository.findByIdAndIsDeletedFalse(couponId)
            ?: throw ApiErrorException(ResponseCodeEnum.NOT_FOUND_COUPON)
    }

    private fun validateDiscountPolicy(
        discountType: CouponDiscountType,
        discountValue: Int,
        maxDiscountPrice: Int?,
    ) {
        val valid = when (discountType) {
            CouponDiscountType.FIXED -> maxDiscountPrice == null
            CouponDiscountType.PERCENTAGE -> discountValue in 1..MAX_DISCOUNT_RATE
        }
        if (!valid) {
            throw ApiErrorException(ResponseCodeEnum.BAD_REQUEST)
        }
    }

    companion object {
        private const val MAX_DISCOUNT_RATE = 100
    }
}
