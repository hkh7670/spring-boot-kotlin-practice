package com.example.springbootkotlinpractice.common.validation

object PasswordPolicy {
    const val PATTERN = """^(?=.*[A-Za-z])(?=.*\d)(?=.*[^A-Za-z0-9]).{10,64}$"""
    const val MESSAGE = "비밀번호는 영문, 숫자, 특수문자를 모두 포함하여 10자 이상 64자 이하로 입력해주세요."
}
