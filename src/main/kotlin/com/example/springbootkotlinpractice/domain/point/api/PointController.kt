package com.example.springbootkotlinpractice.domain.point.api

import com.example.springbootkotlinpractice.common.dto.CommonResponse
import com.example.springbootkotlinpractice.common.dto.ResponseHandler
import com.example.springbootkotlinpractice.common.security.UserPrincipal
import com.example.springbootkotlinpractice.domain.point.dto.PointBalanceResponse
import com.example.springbootkotlinpractice.domain.point.service.PointService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "[POINT] Point", description = "회원 포인트 잔액 조회 API")
@RequestMapping("/api/v1/points")
@RestController
class PointController(
    private val pointService: PointService,
) {

    @Operation(
        summary = "사용 가능한 포인트 잔액 조회 API",
        description = "본인이 보유한 사용 가능(ACTIVE) 포인트의 합계를 조회한다.",
    )
    @PreAuthorize("hasRole('USER')")
    @GetMapping
    fun getUsableAmount(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
    ): ResponseEntity<CommonResponse<PointBalanceResponse>> {
        return ResponseHandler.ok(
            PointBalanceResponse(usableAmount = pointService.getUsableAmount(userPrincipal.id))
        )
    }
}
