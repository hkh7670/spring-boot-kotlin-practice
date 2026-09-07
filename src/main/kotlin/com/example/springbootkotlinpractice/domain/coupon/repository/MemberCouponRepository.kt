package com.example.springbootkotlinpractice.domain.coupon.repository

import com.example.springbootkotlinpractice.domain.coupon.entity.MemberCoupon
import com.example.springbootkotlinpractice.enums.MemberCouponStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface MemberCouponRepository : JpaRepository<MemberCoupon, Long> {

    fun findByIdAndMemberId(id: Long, memberId: Long): MemberCoupon?

    fun findByMemberIdAndStatus(memberId: Long, status: MemberCouponStatus): List<MemberCoupon>

    // UNUSED 상태일 때만 사용 처리(원자적 조건부 UPDATE) — decreaseStock()과 동일 패턴.
    // 0건이면 이미 사용됐거나 존재하지 않는 쿠폰이라는 뜻. 벌크 UPDATE는 JPA Auditing을 안 타므로
    // updated_datetime을 직접 갱신한다
    @Modifying
    @Query(
        value = "UPDATE member_coupons SET status = 'USED', order_id = :orderId, used_at = NOW(6), " +
                "updated_datetime = NOW(6) WHERE id = :id AND member_id = :memberId AND status = 'UNUSED'",
        nativeQuery = true,
    )
    fun useIfUnused(
        @Param("id") id: Long,
        @Param("memberId") memberId: Long,
        @Param("orderId") orderId: Long,
    ): Int

    // 주문취소/반품/결제실패 시 원복 — 이 주문에 실제로 사용된 쿠폰이 있을 때만 되돌린다(없으면 0건, no-op)
    @Modifying
    @Query(
        value = "UPDATE member_coupons SET status = 'UNUSED', order_id = NULL, used_at = NULL, " +
                "updated_datetime = NOW(6) WHERE order_id = :orderId AND status = 'USED'",
        nativeQuery = true,
    )
    fun restoreIfUsedByOrder(@Param("orderId") orderId: Long): Int
}
