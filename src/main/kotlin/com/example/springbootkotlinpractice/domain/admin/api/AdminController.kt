package com.example.springbootkotlinpractice.domain.admin.api

import com.example.springbootkotlinpractice.common.dto.CommonResponse
import com.example.springbootkotlinpractice.common.dto.ResponseHandler
import com.example.springbootkotlinpractice.domain.admin.dto.AdminCreateRequest
import com.example.springbootkotlinpractice.domain.admin.dto.AdminCreateResponse
import com.example.springbootkotlinpractice.domain.admin.service.AdminService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "[ADMIN] Account", description = "관리자 계정 관리 API")
@RequestMapping("/api/v1/admin/accounts")
@RestController
class AdminController(
    private val adminService: AdminService,
) {
    @Operation(
        summary = "관리자 계정 생성 API",
        description = "관리자 계정 생성 API",
    )
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "처리 성공"),
    )
    @PostMapping
    fun createAdmin(
        @RequestBody @Valid request: AdminCreateRequest
    ): ResponseEntity<CommonResponse<AdminCreateResponse>> {
        return ResponseHandler.created(
            adminService.createAdmin(request)
        )
    }
}
