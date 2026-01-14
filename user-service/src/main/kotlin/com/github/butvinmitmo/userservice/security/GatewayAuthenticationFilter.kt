package com.github.butvinmitmo.userservice.security

import com.github.butvinmitmo.shared.security.GatewayAuthenticationToken
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component
class GatewayAuthenticationFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val userIdHeader = request.getHeader("X-User-Id")
        val roleHeader = request.getHeader("X-User-Role")

        if (userIdHeader != null && roleHeader != null) {
            try {
                val userId = UUID.fromString(userIdHeader)
                val authentication = GatewayAuthenticationToken(userId, roleHeader)
                SecurityContextHolder.getContext().authentication = authentication
            } catch (e: IllegalArgumentException) {
                // Invalid UUID format - continue without authentication
            }
        }

        filterChain.doFilter(request, response)
    }
}
