package com.github.butvinmitmo.filesservice.infrastructure.security

import com.github.butvinmitmo.shared.security.GatewayAuthenticationToken
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.util.UUID

@Component
class GatewayAuthenticationWebFilter : WebFilter {
    override fun filter(
        exchange: ServerWebExchange,
        chain: WebFilterChain,
    ): Mono<Void> {
        val userIdHeader = exchange.request.headers.getFirst("X-User-Id")
        val roleHeader = exchange.request.headers.getFirst("X-User-Role")

        return if (userIdHeader != null && roleHeader != null) {
            try {
                val userId = UUID.fromString(userIdHeader)
                val authentication = GatewayAuthenticationToken(userId, roleHeader)
                val securityContext = SecurityContextImpl(authentication)
                chain
                    .filter(
                        exchange,
                    ).contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)))
            } catch (e: IllegalArgumentException) {
                chain.filter(exchange)
            }
        } else {
            chain.filter(exchange)
        }
    }
}
