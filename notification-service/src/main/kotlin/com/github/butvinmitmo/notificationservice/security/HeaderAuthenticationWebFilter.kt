package com.github.butvinmitmo.notificationservice.security

import com.github.butvinmitmo.sharedsecurity.HeaderAuthentication
import com.github.butvinmitmo.sharedsecurity.UserPrincipal
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.util.UUID

/**
 * Reactive filter that extracts X-User-Id and X-User-Role headers (set by gateway)
 * and populates the reactive Spring Security context.
 */
@Component
class HeaderAuthenticationWebFilter : WebFilter {
    override fun filter(
        exchange: ServerWebExchange,
        chain: WebFilterChain,
    ): Mono<Void> {
        val userId = exchange.request.headers.getFirst("X-User-Id")
        val role = exchange.request.headers.getFirst("X-User-Role")

        return if (userId != null && role != null) {
            try {
                val principal = UserPrincipal(UUID.fromString(userId), role)
                val auth = HeaderAuthentication(principal)
                chain
                    .filter(exchange)
                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
            } catch (e: IllegalArgumentException) {
                // Invalid UUID format - continue without authentication
                chain.filter(exchange)
            }
        } else {
            chain.filter(exchange)
        }
    }
}
