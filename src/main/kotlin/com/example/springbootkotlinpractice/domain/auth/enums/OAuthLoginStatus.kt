package com.example.springbootkotlinpractice.domain.auth.enums

enum class OAuthLoginStatus(
    val desc: String,
) {
    LOGIN("로그인 성공"),
    NEED_SIGN_UP("회원가입 필요"),
    ;

    companion object {
        const val API_DOCS_DESC = """
        - Oauth 로그인 처리 결과 상태
          - LOGIN: 로그인 성공
          - NEED_SIGN_UP: 회원가입 필요"""
    }

}
