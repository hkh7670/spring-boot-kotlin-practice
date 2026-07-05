package com.example.springbootkotlinpractice.enums

import org.springframework.http.HttpStatus

enum class ResponseCodeEnum(
    val httpStatus: HttpStatus,
    val resultCode: String,
    val resultMsg: String,
) {
    // Common (0000 ~ 0999)
    OK(HttpStatus.OK, "0000", "OK"),
    CREATED(HttpStatus.CREATED, "0001", "Created"),
    VALIDATE_SUCCESS(HttpStatus.OK, "0002", "입력 값 검증에 성공했습니다."),

    // Custom Response Code (1000 ~ )
    NOT_FOUND_USER(HttpStatus.NOT_FOUND, "1000", "회원정보가 존재하지 않습니다."),
    DUPLICATED_EMAIL(HttpStatus.CONFLICT, "1001", "이미 사용중인 이메일 입니다."),

    // Auth Error (4000 ~)
    INVALID_JWT_TOKEN(HttpStatus.UNAUTHORIZED, "4000", "유효하지 않은 JWT Token 입니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "4001", "유효하지 않은 접근 입니다."),
    INVALID_OAUTH_TOKEN(HttpStatus.UNAUTHORIZED, "4002", "유효하지 않은 OAuth 토큰 입니다."),

    // Client Error (8000 ~)
    SCHEMA_VALIDATE_ERROR(HttpStatus.BAD_REQUEST, "8000", "요청 필드에 대한 검증에 실패하였습니다."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "8001", "유효하지 않은 요청입니다."),
    MISSING_AGE_INFO(HttpStatus.BAD_REQUEST, "8002", "연령 관련 정보가 존재하지 않습니다."),
    NOT_ALLOWED_SHARK_IMAGE_REGENERATE(HttpStatus.BAD_REQUEST, "8003", "아기상어 이미지 재생성이 허용되지 않습니다."),

    // Server Error (9000 ~)
    S3_UPLOADER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "9000", "S3 업로드 중 오류가 발생하였습니다."),
    WATCH_ID_SEQUENCE_OVERFLOW(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "9001",
        "사용 가능한 Watch ID 시퀀스가 존재하지 않습니다. 관리자에게 문의해주세요.",
    ),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "9998", "내부 서버 오류 입니다. 관리자에게 문의해주세요."),
    EXTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "9999", "외부 서버 오류 입니다. 관리자에게 문의해주세요."),
    ;

}
