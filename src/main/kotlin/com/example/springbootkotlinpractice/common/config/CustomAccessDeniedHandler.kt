package com.example.springbootkotlinpractice.common.config

import com.example.springbootkotlinpractice.enums.ResponseCodeEnum
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component

// 403 인가(권한) 실패 처리
@Component
class CustomAccessDeniedHandler(
    private val objectMapper: ObjectMapper
) : AccessDeniedHandler {

    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException,
    ) {
        response.status = HttpServletResponse.SC_FORBIDDEN
        response.contentType = "application/json;charset=utf-8"

        val responseCodeEnum = ResponseCodeEnum.ACCESS_DENIED
        val errorResponse = mapOf(
            "resultCode" to responseCodeEnum.resultCode,
            "resultMsg" to responseCodeEnum.resultMsg,
            "data" to null
        )

        objectMapper.writeValue(
            response.writer,
            errorResponse
        )
    }


}