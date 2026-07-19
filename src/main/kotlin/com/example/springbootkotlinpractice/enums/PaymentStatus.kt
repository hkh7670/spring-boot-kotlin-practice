package com.example.springbootkotlinpractice.enums

enum class PaymentStatus(
    val desc: String,
) {
    READY("결제 요청됨"),
    IN_PROGRESS("결제 진행중"),
    DONE("결제 완료"),
    CANCELED("결제 취소"),
    PARTIAL_CANCELED("부분 취소"),
    ABORTED("결제 승인 실패"),
    EXPIRED("유효 시간 만료"),

}
