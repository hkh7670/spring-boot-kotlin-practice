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
    NOT_FOUND_ORDER(HttpStatus.NOT_FOUND, "1002", "주문 정보가 존재하지 않습니다."),
    NOT_FOUND_PRODUCT(HttpStatus.NOT_FOUND, "1003", "상품 정보가 존재하지 않습니다."),
    NOT_FOUND_DELIVERY_OPTION(HttpStatus.NOT_FOUND, "1004", "배송 옵션 정보가 존재하지 않습니다."),
    NOT_ENOUGH_STOCK(HttpStatus.CONFLICT, "1005", "재고가 부족합니다."),
    ALREADY_PAID_ORDER(HttpStatus.CONFLICT, "1006", "이미 결제가 완료된 주문입니다."),
    PAYMENT_AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST, "1007", "주문 금액과 결제 금액이 일치하지 않습니다."),
    PAYMENT_CONFIRM_FAILED(HttpStatus.BAD_GATEWAY, "1008", "결제 승인에 실패했습니다."),
    ORDER_ALREADY_CANCELLED(HttpStatus.CONFLICT, "1009", "이미 취소된 주문입니다."),
    ORDER_NOT_PAID(HttpStatus.CONFLICT, "1010", "결제가 완료되지 않아 취소할 수 없는 주문입니다."),
    PAYMENT_CANCEL_FAILED(HttpStatus.BAD_GATEWAY, "1011", "결제 취소에 실패했습니다."),
    NOT_FOUND_PAYMENT_INFO(HttpStatus.NOT_FOUND, "1012", "결제 정보가 존재하지 않습니다."),
    ORDER_NOT_SHIPPABLE(HttpStatus.CONFLICT, "1013", "결제 완료 상태의 주문만 배송을 시작할 수 있습니다."),
    ORDER_NOT_SHIPPING(HttpStatus.CONFLICT, "1014", "배송중 상태의 주문만 배송완료 처리할 수 있습니다."),
    ORDER_ALREADY_SHIPPING(HttpStatus.CONFLICT, "1015", "이미 배송이 시작되어 취소할 수 없는 주문입니다."),
    ORDER_NOT_DELIVERED(HttpStatus.CONFLICT, "1016", "배송완료된 주문만 반품을 요청할 수 있습니다."),
    ORDER_NOT_RETURNING(HttpStatus.CONFLICT, "1017", "반품중 상태의 주문만 반품완료 처리할 수 있습니다."),
    ADMIN_ALREADY_WITHDRAWN(HttpStatus.CONFLICT, "1018", "이미 탈퇴한 관리자입니다."),
    ADMIN_ALREADY_INACTIVE(HttpStatus.CONFLICT, "1019", "이미 휴면 상태인 관리자입니다."),
    ADMIN_NOT_INACTIVE(HttpStatus.CONFLICT, "1020", "휴면 상태의 관리자만 활성화할 수 있습니다."),
    NOT_FOUND_CART_ITEM(HttpStatus.NOT_FOUND, "1021", "장바구니에 담긴 상품 정보가 존재하지 않습니다."),
    NOT_FOUND_PRODUCT_OPTION(HttpStatus.NOT_FOUND, "1022", "상품 옵션 정보가 존재하지 않습니다."),
    NOT_FOUND_TOTP_ENROLLMENT(HttpStatus.NOT_FOUND, "1023", "TOTP 등록 요청 정보가 존재하지 않습니다. 등록을 다시 시작해주세요."),
    TOTP_ALREADY_ENABLED(HttpStatus.CONFLICT, "1024", "이미 TOTP 2단계 인증이 활성화되어 있습니다."),
    TOTP_NOT_ENABLED(HttpStatus.CONFLICT, "1025", "TOTP 2단계 인증이 활성화되어 있지 않습니다."),
    NOT_FOUND_MEMBER_COUPON(HttpStatus.NOT_FOUND, "1026", "보유한 쿠폰 정보가 존재하지 않습니다."),
    ALREADY_USED_COUPON(HttpStatus.CONFLICT, "1027", "이미 사용되었거나 사용할 수 없는 쿠폰입니다."),
    EXPIRED_COUPON(HttpStatus.BAD_REQUEST, "1028", "유효기간이 지난 쿠폰입니다."),
    MIN_ORDER_PRICE_NOT_MET(HttpStatus.BAD_REQUEST, "1029", "쿠폰 적용을 위한 최소 주문 금액을 충족하지 않았습니다."),
    NOT_ENOUGH_POINT(HttpStatus.CONFLICT, "1030", "사용 가능한 포인트가 부족합니다."),
    INVALID_DISCOUNT_AMOUNT(HttpStatus.BAD_REQUEST, "1031", "할인 적용 후 결제 금액이 0원 이하가 될 수 없습니다."),

    // Auth Error (4000 ~)
    INVALID_JWT_TOKEN(HttpStatus.UNAUTHORIZED, "4000", "유효하지 않은 JWT Token 입니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "4001", "유효하지 않은 접근 입니다."),
    INVALID_OAUTH_TOKEN(HttpStatus.UNAUTHORIZED, "4002", "유효하지 않은 OAuth 토큰 입니다."),
    OAUTH_EMAIL_REQUIRED(HttpStatus.BAD_REQUEST, "4003", "OAuth 이메일 제공에 동의해주세요."),
    INVALID_TEMP_TOKEN(HttpStatus.UNAUTHORIZED, "4004", "유효하지 않은 임시 토큰 입니다."),
    ALREADY_REGISTERED_OAUTH(HttpStatus.CONFLICT, "4005", "이미 가입된 OAuth 계정입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "4006", "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_TOTP_CODE(HttpStatus.UNAUTHORIZED, "4007", "TOTP 코드가 올바르지 않습니다."),
    INVALID_TOTP_PENDING_TOKEN(HttpStatus.UNAUTHORIZED, "4008", "유효하지 않은 TOTP 인증 토큰입니다."),

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
    SEARCH_ENGINE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "9002", "검색엔진 연동 중 오류가 발생했습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "9998", "내부 서버 오류 입니다. 관리자에게 문의해주세요."),
    EXTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "9999", "외부 서버 오류 입니다. 관리자에게 문의해주세요."),
    ;

}
