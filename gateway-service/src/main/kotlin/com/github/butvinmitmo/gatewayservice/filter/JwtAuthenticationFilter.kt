package com.github.butvinmitmo.gatewayservice.filter

import com.github.butvinmitmo.gatewayservice.config.SecurityProperties
import com.github.butvinmitmo.gatewayservice.security.JwtUtil
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
class JwtAuthenticationFilter(
    private val jwtUtil: JwtUtil,
    private val securityProperties: SecurityProperties,
) : GlobalFilter,
    Ordered {
    override fun filter(
        exchange: ServerWebExchange,
        chain: GatewayFilterChain,
    ): Mono<Void> {
        val path = exchange.request.path.value()

        // Skip public paths
        if (securityProperties.publicPaths.any { path.startsWith(it) }) {
            return chain.filter(exchange)
        }

        // Extract and validate JWT
        val authHeader = exchange.request.headers.getFirst("Authorization")
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.response.statusCode = HttpStatus.UNAUTHORIZED
            return exchange.response.setComplete()
        }

        val token = authHeader.substring(7)
        val claims =
            jwtUtil.validateAndExtract(token) ?: run {
                exchange.response.statusCode = HttpStatus.UNAUTHORIZED
                return exchange.response.setComplete()
            }

        val userId = jwtUtil.getUserId(claims)
        val role = jwtUtil.getRole(claims)
        if (userId == null || role == null) {
            exchange.response.statusCode = HttpStatus.UNAUTHORIZED
            return exchange.response.setComplete()
        }

        // Add headers for backend services
        val mutatedRequest =
            exchange.request
                .mutate()
                .header("X-User-Id", userId.toString())
                .header("X-User-Role", role)
                .build()

        return chain.filter(exchange.mutate().request(mutatedRequest).build())
    }

    override fun getOrder() = Ordered.HIGHEST_PRECEDENCE
}
