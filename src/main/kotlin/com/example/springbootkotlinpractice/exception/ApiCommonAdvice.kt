package com.example.springbootkotlinpractice.exception

import com.example.springbootkotlinpractice.common.dto.CommonResponse
import com.example.springbootkotlinpractice.enums.ResponseCodeEnum
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
class ApiCommonAdvice {

    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(ApiErrorException::class)
    fun <T> handleCustomBaseException(e: ApiErrorException): ResponseEntity<CommonResponse<T>> {
        log.error(e.message, e)
        return ResponseEntity
            .status(e.responseCodeEnum.httpStatus)
            .body(CommonResponse.failResponse(e.responseCodeEnum))
    }

    // RequestBody / ModelAttribute 유효성 에러 처리
    @ExceptionHandler(MethodArgumentNotValidException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleMethodArgumentNotValidException(
        e: MethodArgumentNotValidException,
    ): CommonResponse<ErrorInfoResponse> {
        log.error(e.message, e)
        val errorInfoList = e.bindingResult.fieldErrors.map { ErrorInfo.from(it) }
        return CommonResponse.schemaValidateErrorResponse(errorInfoList)
    }

    // RequestBody JSON 파싱 에러 처리 (날짜 형식 오류 등)
    @ExceptionHandler(HttpMessageNotReadableException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleHttpMessageNotReadableException(e: HttpMessageNotReadableException): CommonResponse<*> {
        log.error(e.message, e)

        val cause = e.cause
        if (cause is MismatchedInputException && cause.path.isNotEmpty()) {
            val fieldName = cause.path.first().fieldName
            return when (cause) {
                // 타입 불일치 (날짜/숫자/Enum 등)
                is InvalidFormatException -> CommonResponse.schemaValidateErrorResponse(
                    listOf(ErrorInfo.fromInvalidFormat(fieldName, cause.value, cause.targetType)),
                )
                // 필수 필드 누락 / null (Kotlin non-null 파라미터 미충족 포함)
                else -> CommonResponse.schemaValidateErrorResponse(
                    listOf(ErrorInfo.of(fieldName, null, "필수 입력값 입니다.")),
                )
            }
        }

        return CommonResponse.failResponse<Nothing>(ResponseCodeEnum.BAD_REQUEST)
    }

    // QueryString 타입 불일치 에러 처리
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleMethodArgumentTypeMismatchException(
        e: MethodArgumentTypeMismatchException,
    ): CommonResponse<ErrorInfoResponse> {
        log.error(e.message, e)
        return CommonResponse.schemaValidateErrorResponse(
            listOf(ErrorInfo.fromInvalidFormat(e.name, e.value, e.requiredType)),
        )
    }

    // QueryString Bean Validation 에러 처리
    @ExceptionHandler(HandlerMethodValidationException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleHandlerMethodValidationException(
        e: HandlerMethodValidationException,
    ): CommonResponse<ErrorInfoResponse> {
        log.error(e.message, e)
        val errorInfoList = e.parameterValidationResults.flatMap { result ->
            val fieldName = result.methodParameter.parameterName
            val fieldValue = result.argument
            result.resolvableErrors.map { error ->
                ErrorInfo.of(fieldName, fieldValue, error.defaultMessage)
            }
        }
        return CommonResponse.schemaValidateErrorResponse(errorInfoList)
    }

    // 메서드 시큐리티(@PreAuthorize) 인가 실패 처리 (AuthorizationDeniedException 포함)
    @ExceptionHandler(AccessDeniedException::class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    fun <T> handleAccessDeniedException(e: AccessDeniedException): CommonResponse<T> {
        log.warn(e.message, e)
        return CommonResponse.failResponse(ResponseCodeEnum.ACCESS_DENIED)
    }

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun <T> handleException(e: Exception): CommonResponse<T> {
        log.error(e.message, e)
        return CommonResponse.internalServerErrorResponse()
    }
}
