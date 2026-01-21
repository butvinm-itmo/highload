package com.github.butvinmitmo.filesservice.config

import com.github.butvinmitmo.filesservice.infrastructure.security.GatewayAuthenticationWebFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity(useAuthorizationManager = true)
class SecurityConfig(
    private val gatewayAuthenticationWebFilter: GatewayAuthenticationWebFilter,
) {
    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain =
        http
            .csrf { it.disable() }
            .authorizeExchange {
                it
                    // Internal endpoints - no auth required for service-to-service calls
                    .pathMatchers("/internal/**")
                    .permitAll()
                    // Health and API docs
                    .pathMatchers("/actuator/**", "/api-docs/**")
                    .permitAll()
                    // All other requests require authentication
                    .anyExchange()
                    .authenticated()
            }.addFilterAt(gatewayAuthenticationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            .build()
}
