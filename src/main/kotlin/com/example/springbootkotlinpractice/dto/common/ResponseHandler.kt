package com.example.springbootkotlinpractice.dto.common

import com.example.springbootkotlinpractice.enums.ResponseCodeEnum
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

class ResponseHandler {
    companion object {

        // 200 응답 (Response O)
        fun <T> ok(response: T): ResponseEntity<CommonResponse<T>> {
            return ResponseEntity.ok(CommonResponse.successResponse(response))
        }

        // 200 응답 (Response X)
        fun <T> ok(): ResponseEntity<CommonResponse<T>> {
            return ResponseEntity.ok(CommonResponse.successResponse())
        }

        // 201 Created 응답 (Response O)
        fun <T> created(response: T): ResponseEntity<CommonResponse<T>> {
            return ResponseEntity(CommonResponse.createdResponse(response), HttpStatus.CREATED)
        }

        // 201 Created 응답 (Response X)
        fun <T> created(): ResponseEntity<CommonResponse<T>> {
            return ResponseEntity(CommonResponse.createdResponse(), HttpStatus.CREATED)
        }

        // 실패 응답
        fun <T> fail(
            httpStatus: HttpStatus,
            responseCodeEnum: ResponseCodeEnum,
        ): ResponseEntity<CommonResponse<T>> {
            return ResponseEntity(CommonResponse.failResponse(responseCodeEnum), httpStatus)
        }
    }
}
