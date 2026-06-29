package com.example.springbootkotlinpractice.common.dto

import com.example.springbootkotlinpractice.enums.ResponseCodeEnum
import com.example.springbootkotlinpractice.exception.ErrorInfo
import com.example.springbootkotlinpractice.exception.ErrorInfoResponse

data class CommonResponse<T>(
    val resultCode: String,
    val resultMsg: String,
    val data: T? = null,
) {
    companion object {
        fun <T> successResponse(): CommonResponse<T> {
            return CommonResponse(
                resultCode = ResponseCodeEnum.OK.resultCode,
                resultMsg = ResponseCodeEnum.OK.resultMsg,
            )
        }

        fun <T> successResponse(data: T): CommonResponse<T> {
            return CommonResponse(
                resultCode = ResponseCodeEnum.OK.resultCode,
                resultMsg = ResponseCodeEnum.OK.resultMsg,
                data = data,
            )
        }

        fun <T> createdResponse(data: T): CommonResponse<T> {
            return CommonResponse(
                resultCode = ResponseCodeEnum.CREATED.resultCode,
                resultMsg = ResponseCodeEnum.CREATED.resultMsg,
                data = data,
            )
        }

        fun <T> createdResponse(): CommonResponse<T> {
            return CommonResponse(
                resultCode = ResponseCodeEnum.CREATED.resultCode,
                resultMsg = ResponseCodeEnum.CREATED.resultMsg,
            )
        }

        fun <T> response(responseCodeEnum: ResponseCodeEnum, data: T?): CommonResponse<T> {
            return CommonResponse(
                resultCode = responseCodeEnum.resultCode,
                resultMsg = responseCodeEnum.resultMsg,
                data = data,
            )
        }

        fun <T> failResponse(responseCodeEnum: ResponseCodeEnum): CommonResponse<T> {
            return CommonResponse(
                resultCode = responseCodeEnum.resultCode,
                resultMsg = responseCodeEnum.resultMsg,
                data = null,
            )
        }

        fun schemaValidateErrorResponse(errorInfoList: List<ErrorInfo>): CommonResponse<ErrorInfoResponse> {
            return CommonResponse(
                resultCode = ResponseCodeEnum.SCHEMA_VALIDATE_ERROR.resultCode,
                resultMsg = ResponseCodeEnum.SCHEMA_VALIDATE_ERROR.resultMsg,
                data = ErrorInfoResponse(errorInfoList),
            )
        }

        fun <T> internalServerErrorResponse(): CommonResponse<T> {
            return CommonResponse(
                resultCode = ResponseCodeEnum.INTERNAL_SERVER_ERROR.resultCode,
                resultMsg = ResponseCodeEnum.INTERNAL_SERVER_ERROR.resultMsg,
                data = null,
            )
        }
    }
}
