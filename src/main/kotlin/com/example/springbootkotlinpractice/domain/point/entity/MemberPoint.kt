package com.example.springbootkotlinpractice.domain.point.entity

import com.example.springbootkotlinpractice.common.entity.BaseTimeEntity
import com.example.springbootkotlinpractice.enums.MemberPointStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

// 잔액 차감/원복은 MemberPointRepository.deductIfEnough()/restore()(조건부 UPDATE)로 처리한다 —
// decreaseStock()/increaseStock()과 동일한 컨벤션. 이 엔티티의 remainingAmount/status는 조회용일 뿐,
// 여기서 직접 값을 바꾸지 않는다.
@Entity
@Table(name = "member_points", comment = "회원별 포인트 적립 내역 (잔액 관리)")
class MemberPoint(

    @Column(
        name = "member_id", nullable = false, updatable = false,
        comment = "포인트를 보유한 회원 ID (members.id)",
    )
    val memberId: Long,

    @Column(name = "point_id", nullable = false, updatable = false, comment = "포인트 템플릿 ID (points.id)")
    val pointId: Long,

    @Column(name = "amount", nullable = false, updatable = false, comment = "원 적립 금액")
    val amount: Int,

    @Column(name = "remaining_amount", nullable = false, comment = "남은 사용 가능 금액")
    var remainingAmount: Int,

    @Enumerated(EnumType.STRING)
    @Column(
        name = "status", nullable = false, length = 20,
        comment = "포인트 상태 (ACTIVE/EXHAUSTED/EXPIRED)",
    )
    var status: MemberPointStatus = MemberPointStatus.ACTIVE,

    @Column(name = "issued_at", nullable = false, updatable = false, comment = "적립 일시")
    val issuedAt: LocalDateTime = LocalDateTime.now(),

    @Column(
        name = "expired_at", nullable = true, updatable = false,
        comment = "사용 가능 마감 일시 (NULL이면 무제한)",
    )
    val expiredAt: LocalDateTime? = null,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0L

    companion object {
        fun of(
            memberId: Long,
            pointId: Long,
            amount: Int,
            expiredAt: LocalDateTime? = null,
        ): MemberPoint {
            return MemberPoint(
                memberId = memberId,
                pointId = pointId,
                amount = amount,
                remainingAmount = amount,
                expiredAt = expiredAt,
            )
        }
    }
}
