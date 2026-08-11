package com.example.springbootkotlinpractice.common.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

// SecurityConfig 안에 두면 SecurityConfig → OAuth2LoginSuccessHandler → AuthService → PasswordEncoder
// (SecurityConfig 자기 자신의 @Bean) 순으로 순환 참조가 생겨 별도 설정으로 분리했다
@Configuration
class PasswordEncoderConfig {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}
