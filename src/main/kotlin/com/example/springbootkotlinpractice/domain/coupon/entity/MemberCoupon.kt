package com.example.springbootkotlinpractice.domain.coupon.entity

import com.example.springbootkotlinpractice.common.entity.BaseTimeEntity
import com.example.springbootkotlinpractice.enums.MemberCouponStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import org.hibernate.annotations.Comment

// 원자적 상태 전이(사용 처리/원복)는 MemberCouponRepository.useIfUnused()/restoreIfUsedByOrder()
// (조건부 UPDATE)로 처리한다 — Order.status 전환과 동일한 컨벤션. 이 엔티티의 status는 조회용일 뿐,
// 여기서 직접 값을 바꾸지 않는다.
@Entity
@Table(name = "member_coupons")
@Comment("회원별 쿠폰 발급/보유 내역")
class MemberCoupon(

    @Comment("쿠폰을 보유한 회원 ID (members.id)")
    @Column(name = "member_id", nullable = false, updatable = false)
    val memberId: Long,

    @Comment("쿠폰 템플릿 ID (coupons.id)")
    @Column(name = "coupon_id", nullable = false, updatable = false)
    val couponId: Long,

    @Comment("쿠폰 상태 (UNUSED/USED/EXPIRED)")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: MemberCouponStatus = MemberCouponStatus.UNUSED,

    @Comment("사용된 주문 ID (orders.id, 미사용 시 NULL)")
    @Column(name = "order_id", nullable = true)
    var orderId: Long? = null,

    @Comment("발급 일시")
    @Column(name = "issued_at", nullable = false, updatable = false)
    val issuedAt: LocalDateTime = LocalDateTime.now(),

    @Comment("사용 일시 (미사용 시 NULL)")
    @Column(name = "used_at", nullable = true)
    var usedAt: LocalDateTime? = null,

    @Comment("이 발급건의 사용 가능 마감 일시")
    @Column(name = "expired_at", nullable = false, updatable = false)
    val expiredAt: LocalDateTime,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0L

    companion object {
        fun of(
            memberId: Long,
            couponId: Long,
            expiredAt: LocalDateTime,
        ): MemberCoupon {
            return MemberCoupon(
                memberId = memberId,
                couponId = couponId,
                expiredAt = expiredAt,
            )
        }
    }
}
