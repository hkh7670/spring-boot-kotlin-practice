package com.example.springbootkotlinpractice.domain.point.api

import com.example.springbootkotlinpractice.common.dto.CommonResponse
import com.example.springbootkotlinpractice.common.dto.PageResponse
import com.example.springbootkotlinpractice.common.dto.ResponseHandler
import com.example.springbootkotlinpractice.common.security.UserPrincipal
import com.example.springbootkotlinpractice.domain.point.dto.AdminPointRequest
import com.example.springbootkotlinpractice.domain.point.dto.AdminPointResponse
import com.example.springbootkotlinpractice.domain.point.dto.PointGrantRequest
import com.example.springbootkotlinpractice.domain.point.dto.PointGrantResponse
import com.example.springbootkotlinpractice.domain.point.service.AdminPointService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "[ADMIN] Point", description = "관리자 포인트 템플릿 관리 / 회원 적립 API")
@RequestMapping("/api/v1/admin/points")
@RestController
class AdminPointController(
    private val adminPointService: AdminPointService,
) {

    @Operation(
        summary = "포인트 템플릿 생성 API",
        description = "포인트 지급 사유/종류와 적립일로부터의 유효 일수(선택)를 등록한다.",
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    fun createPoint(
        @Valid @RequestBody request: AdminPointRequest,
    ): ResponseEntity<CommonResponse<AdminPointResponse>> {
        return ResponseHandler.created(adminPointService.createPoint(request))
    }

    @Operation(
        summary = "포인트 템플릿 목록 조회 API",
        description = "삭제되지 않은 포인트 템플릿을 최신순으로 페이징 조회한다.",
    )
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    fun getPoints(
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
    ): ResponseEntity<CommonResponse<PageResponse<AdminPointResponse>>> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"))
        return ResponseHandler.ok(adminPointService.getPoints(pageable))
    }

    @Operation(summary = "포인트 템플릿 단건 조회 API")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{pointId}")
    fun getPoint(@PathVariable pointId: Long): ResponseEntity<CommonResponse<AdminPointResponse>> {
        return ResponseHandler.ok(adminPointService.getPoint(pointId))
    }

    @Operation(
        summary = "포인트 템플릿 수정 API",
        description = "이름과 유효 일수를 요청 값으로 교체한다. 이미 적립된 포인트의 만료 일시에는 영향이 없다.",
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{pointId}")
    fun updatePoint(
        @PathVariable pointId: Long,
        @Valid @RequestBody request: AdminPointRequest,
    ): ResponseEntity<CommonResponse<AdminPointResponse>> {
        return ResponseHandler.ok(adminPointService.updatePoint(pointId, request))
    }

    @Operation(
        summary = "포인트 템플릿 삭제 API",
        description = "포인트 템플릿을 soft delete 처리해 신규 적립을 중단한다. 이미 적립된 포인트는 만료일까지 사용할 수 있다.",
    )
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{pointId}")
    fun deletePoint(@PathVariable pointId: Long): ResponseEntity<CommonResponse<Unit>> {
        adminPointService.deletePoint(pointId)
        return ResponseHandler.ok()
    }

    @Operation(
        summary = "포인트 회원 지정 적립 API",
        description = "지정한 회원들에게 같은 금액을 적립한다. 존재하지 않는 회원이 하나라도 있으면 " +
                "전체 요청이 실패하고 아무에게도 적립되지 않는다.",
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{pointId}/grant")
    fun grantPoint(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable pointId: Long,
        @Valid @RequestBody request: PointGrantRequest,
    ): ResponseEntity<CommonResponse<PointGrantResponse>> {
        return ResponseHandler.created(
            adminPointService.grantPoint(userPrincipal.id, pointId, request)
        )
    }
}
