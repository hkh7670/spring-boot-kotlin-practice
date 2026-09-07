package com.example.springbootkotlinpractice.domain.coupon.service

import com.example.springbootkotlinpractice.domain.coupon.dto.MemberCouponResponse
import com.example.springbootkotlinpractice.domain.coupon.repository.CouponRepository
import com.example.springbootkotlinpractice.domain.coupon.repository.MemberCouponRepository
import com.example.springbootkotlinpractice.enums.MemberCouponStatus
import com.example.springbootkotlinpractice.enums.ResponseCodeEnum
import com.example.springbootkotlinpractice.exception.ApiErrorException
import java.time.LocalDateTime
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CouponService(
    private val couponRepository: CouponRepository,
    private val memberCouponRepository: MemberCouponRepository,
) {

    // 상품 목록의 vendorName 배치조회(ProductService)와 동일한 패턴 — 보유 쿠폰들의 템플릿을
    // findAllById()로 한 번에 조회 후 Map으로 매칭해 N+1을 피한다
    @Transactional(readOnly = true)
    fun getUsableCoupons(memberId: Long): List<MemberCouponResponse> {
        val memberCoupons = memberCouponRepository.findByMemberIdAndStatus(memberId, MemberCouponStatus.UNUSED)
        val couponMap = couponRepository.findAllById(memberCoupons.map { it.couponId }).associateBy { it.id }

        return memberCoupons.mapNotNull { memberCoupon ->
            val coupon = couponMap[memberCoupon.couponId] ?: return@mapNotNull null
            MemberCouponResponse(
                memberCouponId = memberCoupon.id,
                couponName = coupon.name,
                discountType = coupon.discountType,
                discountValue = coupon.discountValue,
                maxDiscountPrice = coupon.maxDiscountPrice,
                minOrderPrice = coupon.minOrderPrice,
                expiredAt = memberCoupon.expiredAt,
            )
        }
    }

    // 주문 생성 시 쿠폰 사용을 확정하고 실제 할인 금액을 반환한다. 검증 실패 시 예외를 던져
    // OrderService.createOrder()의 트랜잭션이 통째로 롤백되도록 한다(재고차감 포함)
    @Transactional
    fun use(memberId: Long, memberCouponId: Long, orderId: Long, productTotalPrice: Int): Int {
        val memberCoupon = memberCouponRepository.findByIdAndMemberId(memberCouponId, memberId)
            ?: throw ApiErrorException(ResponseCodeEnum.NOT_FOUND_MEMBER_COUPON)
        if (memberCoupon.status != MemberCouponStatus.UNUSED) {
            throw ApiErrorException(ResponseCodeEnum.ALREADY_USED_COUPON)
        }
        if (memberCoupon.expiredAt.isBefore(LocalDateTime.now())) {
            throw ApiErrorException(ResponseCodeEnum.EXPIRED_COUPON)
        }

        val coupon = couponRepository.findByIdOrNull(memberCoupon.couponId)
            ?: throw ApiErrorException(ResponseCodeEnum.NOT_FOUND_MEMBER_COUPON)
        if (productTotalPrice < coupon.minOrderPrice) {
            throw ApiErrorException(ResponseCodeEnum.MIN_ORDER_PRICE_NOT_MET)
        }

        // 동시 중복 사용 방어 — 원자적 조건부 UPDATE로 UNUSED일 때만 확정한다.
        // 0건이면 위 검증 이후 다른 요청이 먼저 써버렸다는 뜻
        val updatedRows = memberCouponRepository.useIfUnused(memberCouponId, memberId, orderId)
        if (updatedRows == 0) {
            throw ApiErrorException(ResponseCodeEnum.ALREADY_USED_COUPON)
        }

        return coupon.calculateDiscountPrice(productTotalPrice)
    }

    // 주문취소/반품/결제실패 시 원복 — 이 주문에 실제로 사용된 쿠폰이 없으면 내부적으로 no-op이다
    @Transactional
    fun restore(orderId: Long) {
        memberCouponRepository.restoreIfUsedByOrder(orderId)
    }
}
