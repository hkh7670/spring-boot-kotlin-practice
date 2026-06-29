package com.example.springbootkotlinpractice.common.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val token = resolveToken(request)
        if (token != null && jwtTokenProvider.isValid(token)) {
            val memberId = jwtTokenProvider.getMemberId(token)
            val role = jwtTokenProvider.getRole(token)
            val principal = UserPrincipal(id = memberId, role = role)
            val authorities =
                listOf(SimpleGrantedAuthority("ROLE_${role.name}"))
            val authentication = UsernamePasswordAuthenticationToken(
                principal,
                null,
                authorities,
            )
            SecurityContextHolder.getContext().authentication = authentication
        }
        filterChain.doFilter(request, response)
    }

    private fun resolveToken(request: HttpServletRequest): String? {
        val header = request.getHeader(HttpHeaders.AUTHORIZATION) ?: return null
        return if (header.startsWith(BEARER_PREFIX)) header.removePrefix(BEARER_PREFIX).trim() else null
    }

    companion object {
        private const val BEARER_PREFIX = "Bearer "
    }
}
