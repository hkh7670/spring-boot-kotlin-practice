package com.example.springbootkotlinpractice.domain.auth.enums

enum class EmailLoginStatus(
    val desc: String,
) {
    LOGIN("로그인 성공"),
    NEED_TOTP("TOTP 인증 필요"),
    ;

    companion object {
        const val API_DOCS_DESC = """
        - Email 로그인 처리 결과 상태
          - LOGIN: 로그인 성공
          - NEED_TOTP: TOTP 2단계 인증 필요"""
    }

}
