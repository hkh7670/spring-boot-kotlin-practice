package com.example.springbootkotlinpractice.domain.point.entity

import com.example.springbootkotlinpractice.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

// 포인트 사용 내역 (append-only) — 주문취소/반품/결제실패 시 order_id로 조회해 각 memberPointId에
// usedAmount만큼 원복한다. 재고 히스토리(OrderStatusHistory)처럼 FK 제약은 의도적으로 두지 않는다.
@Entity
@Table(name = "member_point_usages", comment = "포인트 사용 내역 (주문 취소/반품 시 원복 기준)")
class MemberPointUsage(

    @Column(
        name = "order_id", nullable = false, updatable = false,
        comment = "포인트를 사용한 주문 ID (orders.id)",
    )
    val orderId: Long,

    @Column(
        name = "member_point_id", nullable = false, updatable = false,
        comment = "차감된 포인트 적립건 ID (member_points.id)",
    )
    val memberPointId: Long,

    @Column(name = "used_amount", nullable = false, updatable = false, comment = "이 적립건에서 사용한 금액")
    val usedAmount: Int,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0L

    companion object {
        fun of(orderId: Long, memberPointId: Long, usedAmount: Int): MemberPointUsage {
            return MemberPointUsage(
                orderId = orderId,
                memberPointId = memberPointId,
                usedAmount = usedAmount,
            )
        }
    }
}
