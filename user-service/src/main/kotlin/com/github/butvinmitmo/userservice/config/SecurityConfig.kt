package com.github.butvinmitmo.userservice.config

import com.github.butvinmitmo.userservice.security.GatewayAuthenticationWebFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.server.SecurityWebFilterChain

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity(useAuthorizationManager = true)
class SecurityConfig(
    private val gatewayAuthenticationWebFilter: GatewayAuthenticationWebFilter,
) {
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder(10)

    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain =
        http
            .csrf { it.disable() }
            .authorizeExchange {
                it
                    .pathMatchers("/api/v0.0.1/auth/**", "/actuator/**", "/api-docs/**")
                    .permitAll()
                    .anyExchange()
                    .authenticated()
            }.addFilterAt(gatewayAuthenticationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            .build()
}
