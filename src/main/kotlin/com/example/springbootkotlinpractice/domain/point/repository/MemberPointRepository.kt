package com.example.springbootkotlinpractice.domain.point.repository

import com.example.springbootkotlinpractice.domain.point.entity.MemberPoint
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface MemberPointRepository : JpaRepository<MemberPoint, Long> {

    // 사용 가능한 적립건을 만료 임박 순으로 정렬 — FIFO 소진 순서. 무제한(expiredAt=NULL)은 가장 뒤로 밀린다
    @Query(
        "SELECT mp FROM MemberPoint mp WHERE mp.memberId = :memberId AND mp.status = 'ACTIVE' " +
                "AND mp.remainingAmount > 0 " +
                "ORDER BY CASE WHEN mp.expiredAt IS NULL THEN 1 ELSE 0 END, mp.expiredAt ASC"
    )
    fun findUsableByMemberIdOrderByExpiredAtAsc(@Param("memberId") memberId: Long): List<MemberPoint>

    @Query(
        "SELECT COALESCE(SUM(mp.remainingAmount), 0) FROM MemberPoint mp " +
                "WHERE mp.memberId = :memberId AND mp.status = 'ACTIVE'"
    )
    fun sumRemainingAmountByMemberId(@Param("memberId") memberId: Long): Long

    // 잔액이 충분할 때만 원자적으로 차감(초과 사용 방지) — decreaseStock()과 동일 패턴.
    // 잔액이 0이 되면 상태를 EXHAUSTED로 같이 갱신한다
    @Modifying
    @Query(
        value = "UPDATE member_points SET remaining_amount = remaining_amount - :amount, " +
                "status = CASE WHEN remaining_amount - :amount <= 0 THEN 'EXHAUSTED' ELSE status END, " +
                "updated_datetime = NOW(6) WHERE id = :id AND remaining_amount >= :amount",
        nativeQuery = true,
    )
    fun deductIfEnough(@Param("id") id: Long, @Param("amount") amount: Int): Int

    // 주문취소/반품/결제실패 시 원복 — 소진(EXHAUSTED)됐던 건도 다시 ACTIVE로 되돌린다
    @Modifying
    @Query(
        value = "UPDATE member_points SET remaining_amount = remaining_amount + :amount, " +
                "status = 'ACTIVE', updated_datetime = NOW(6) WHERE id = :id",
        nativeQuery = true,
    )
    fun restore(@Param("id") id: Long, @Param("amount") amount: Int): Int
}
