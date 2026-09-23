package com.example.springbootkotlinpractice.domain.point.service

import com.example.springbootkotlinpractice.common.Logging
import com.example.springbootkotlinpractice.common.dto.PageResponse
import com.example.springbootkotlinpractice.domain.member.repository.MemberRepository
import com.example.springbootkotlinpractice.domain.point.dto.AdminPointRequest
import com.example.springbootkotlinpractice.domain.point.dto.AdminPointResponse
import com.example.springbootkotlinpractice.domain.point.dto.PointGrantRequest
import com.example.springbootkotlinpractice.domain.point.dto.PointGrantResponse
import com.example.springbootkotlinpractice.domain.point.entity.MemberPoint
import com.example.springbootkotlinpractice.domain.point.entity.Point
import com.example.springbootkotlinpractice.domain.point.repository.MemberPointRepository
import com.example.springbootkotlinpractice.domain.point.repository.PointRepository
import com.example.springbootkotlinpractice.enums.ResponseCodeEnum
import com.example.springbootkotlinpractice.exception.ApiErrorException
import java.time.LocalDateTime
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminPointService(
    private val pointRepository: PointRepository,
    private val memberPointRepository: MemberPointRepository,
    private val memberRepository: MemberRepository,
) : Logging {

    @Transactional
    fun createPoint(request: AdminPointRequest): AdminPointResponse {
        val point = pointRepository.save(Point.of(request.name, request.validDays))
        return AdminPointResponse.from(point)
    }

    @Transactional(readOnly = true)
    fun getPoints(pageable: Pageable): PageResponse<AdminPointResponse> {
        return PageResponse.of(
            pointRepository.findAllByIsDeletedFalse(pageable).map { AdminPointResponse.from(it) }
        )
    }

    @Transactional(readOnly = true)
    fun getPoint(pointId: Long): AdminPointResponse {
        return AdminPointResponse.from(getActivePoint(pointId))
    }

    @Transactional
    fun updatePoint(pointId: Long, request: AdminPointRequest): AdminPointResponse {
        val point = getActivePoint(pointId)
        point.update(request.name, request.validDays)
        return AdminPointResponse.from(point)
    }

    // 삭제는 신규 적립만 중단한다. 이미 적립된 포인트는 만료일까지 계속 사용할 수 있다
    @Transactional
    fun deletePoint(pointId: Long) {
        getActivePoint(pointId).delete()
    }

    @Transactional
    fun grantPoint(adminId: Long, pointId: Long, request: PointGrantRequest): PointGrantResponse {
        val point = getActivePoint(pointId)

        val memberIds = request.memberIds.distinct()
        if (memberRepository.countByIdIn(memberIds) != memberIds.size.toLong()) {
            throw ApiErrorException(ResponseCodeEnum.NOT_FOUND_USER)
        }

        val expiredAt = point.validDays?.let { LocalDateTime.now().plusDays(it.toLong()) }
        memberPointRepository.saveAll(
            memberIds.map { MemberPoint.of(it, pointId, request.amount, expiredAt) }
        )

        // 금전적 혜택이 나가는 작업이라 누가 누구에게 얼마를 적립했는지 감사 로그를 남긴다
        logger.info(
            "[ADMIN] 포인트 적립 adminId={} pointId={} amount={} memberIds={}",
            adminId, pointId, request.amount, memberIds,
        )
        return PointGrantResponse(grantedCount = memberIds.size)
    }

    private fun getActivePoint(pointId: Long): Point {
        return pointRepository.findByIdAndIsDeletedFalse(pointId)
            ?: throw ApiErrorException(ResponseCodeEnum.NOT_FOUND_POINT)
    }
}
