package com.example.springbootkotlinpractice.common.config

import com.example.springbootkotlinpractice.common.security.JwtAuthenticationFilter
import org.springframework.boot.autoconfigure.security.servlet.PathRequest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val customAccessDeniedHandler: CustomAccessDeniedHandler,
    private val customAuthenticationEntryPoint: CustomAuthenticationEntryPoint,
) {
    companion object {
        private val SWAGGER_PATHS = arrayOf(
            "/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
        )

        private val PERMIT_ALL_PATHS = arrayOf(
            "/api/v1/members/signup/**",
            "/api/v1/members/oauth/**",
        )
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            // h2-console 은 iframe 으로 렌더링되므로 sameOrigin 허용
            .headers { headers -> headers.frameOptions { it.sameOrigin() } }
            .authorizeHttpRequests {
                // Swagger
                it.requestMatchers(*SWAGGER_PATHS).permitAll()
                // H2 Console
                it.requestMatchers(PathRequest.toH2Console()).permitAll()
                // Permit All Paths
                it.requestMatchers(*PERMIT_ALL_PATHS).permitAll()
                it.requestMatchers("/api/v1/members/myself-admin").hasRole("ADMIN")
                // 그 외 전부 인증 필요
                it.anyRequest().authenticated()
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .exceptionHandling { authenticationException ->
                authenticationException
                    .authenticationEntryPoint(customAuthenticationEntryPoint)
                    .accessDeniedHandler(customAccessDeniedHandler)
            }
        return http.build()
    }
}
