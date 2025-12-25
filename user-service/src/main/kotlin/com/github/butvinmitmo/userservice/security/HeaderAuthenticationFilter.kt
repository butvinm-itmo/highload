package com.github.butvinmitmo.userservice.security

import com.github.butvinmitmo.sharedsecurity.HeaderAuthentication
import com.github.butvinmitmo.sharedsecurity.UserPrincipal
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

/**
 * Filter that extracts X-User-Id and X-User-Role headers (set by gateway)
 * and populates the Spring Security context.
 */
@Component
class HeaderAuthenticationFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val userId = request.getHeader("X-User-Id")
        val role = request.getHeader("X-User-Role")

        if (userId != null && role != null) {
            try {
                val principal = UserPrincipal(UUID.fromString(userId), role)
                val auth = HeaderAuthentication(principal)
                SecurityContextHolder.getContext().authentication = auth
            } catch (e: IllegalArgumentException) {
                // Invalid UUID format - ignore and continue without authentication
            }
        }

        filterChain.doFilter(request, response)
    }
}
