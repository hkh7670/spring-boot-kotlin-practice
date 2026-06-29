package com.example.springbootkotlinpractice.common.config

import com.example.springbootkotlinpractice.enums.ResponseCodeEnum
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component

// 401 인증 실패 처리
@Component
class CustomAuthenticationEntryPoint(
    private val objectMapper: ObjectMapper
) : AuthenticationEntryPoint {
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) {
        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = "application/json;charset=utf-8"

        val responseCodeEnum = ResponseCodeEnum.INVALID_JWT_TOKEN
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