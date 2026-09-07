package com.example.springbootkotlinpractice.domain.point.service

import com.example.springbootkotlinpractice.domain.point.entity.MemberPointUsage
import com.example.springbootkotlinpractice.domain.point.repository.MemberPointRepository
import com.example.springbootkotlinpractice.domain.point.repository.MemberPointUsageRepository
import com.example.springbootkotlinpractice.enums.ResponseCodeEnum
import com.example.springbootkotlinpractice.exception.ApiErrorException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PointService(
    private val memberPointRepository: MemberPointRepository,
    private val memberPointUsageRepository: MemberPointUsageRepository,
) {

    @Transactional(readOnly = true)
    fun getUsableAmount(memberId: Long): Long {
        return memberPointRepository.sumRemainingAmountByMemberId(memberId)
    }

    // 만료 임박 순(FIFO)으로 여러 적립건에 걸쳐 부분 차감한다. 사용한 만큼 MemberPointUsage에 남겨둬야
    // 나중에 취소/반품 시 정확히 어느 적립건에서 얼마를 되돌려야 하는지 알 수 있다.
    // 도중에 잔액이 모자라 다 못 채우면 예외를 던져 OrderService.createOrder()의 트랜잭션 전체가
    // 롤백되게 한다 — 여기서 이미 차감한 부분도 함께 자동으로 되돌아가므로 별도 보정 로직이 필요 없다
    @Transactional
    fun use(memberId: Long, orderId: Long, requestAmount: Int): Int {
        if (requestAmount <= 0) {
            return 0
        }

        var remainingToDeduct = requestAmount
        val usablePoints = memberPointRepository.findUsableByMemberIdOrderByExpiredAtAsc(memberId)
        for (memberPoint in usablePoints) {
            if (remainingToDeduct <= 0) {
                break
            }
            val deductAmount = minOf(remainingToDeduct, memberPoint.remainingAmount)
            val updatedRows = memberPointRepository.deductIfEnough(memberPoint.id, deductAmount)
            if (updatedRows > 0) {
                memberPointUsageRepository.save(MemberPointUsage.of(orderId, memberPoint.id, deductAmount))
                remainingToDeduct -= deductAmount
            }
        }

        if (remainingToDeduct > 0) {
            throw ApiErrorException(ResponseCodeEnum.NOT_ENOUGH_POINT)
        }
        return requestAmount
    }

    // 주문취소/반품/결제실패 시 원복 — 이 주문에서 사용한 내역을 조회해 각 적립건에 그대로 되돌려준다.
    // 사용한 적이 없으면 조회 결과가 비어 있어 내부적으로 no-op이다
    @Transactional
    fun restore(orderId: Long) {
        val usages = memberPointUsageRepository.findByOrderId(orderId)
        usages.forEach { usage ->
            memberPointRepository.restore(usage.memberPointId, usage.usedAmount)
        }
        memberPointUsageRepository.deleteAll(usages)
    }
}
