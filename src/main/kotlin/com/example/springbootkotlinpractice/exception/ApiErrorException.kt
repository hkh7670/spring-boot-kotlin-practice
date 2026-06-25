package com.example.springbootkotlinpractice.exception

import com.example.springbootkotlinpractice.enums.ResponseCodeEnum

class ApiErrorException(
    val responseCodeEnum: ResponseCodeEnum,
) : RuntimeException(responseCodeEnum.resultMsg)
