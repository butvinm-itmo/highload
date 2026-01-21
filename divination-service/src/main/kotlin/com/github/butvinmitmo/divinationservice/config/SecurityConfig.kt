package com.github.butvinmitmo.divinationservice.config

import com.github.butvinmitmo.divinationservice.infrastructure.security.GatewayAuthenticationWebFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
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
                    .pathMatchers("/internal/**", "/actuator/**", "/api-docs/**")
                    .permitAll()
                    .pathMatchers(HttpMethod.GET, "/api/v0.0.1/spreads/**")
                    .permitAll()
                    .anyExchange()
                    .authenticated()
            }.addFilterAt(gatewayAuthenticationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            .build()
}
