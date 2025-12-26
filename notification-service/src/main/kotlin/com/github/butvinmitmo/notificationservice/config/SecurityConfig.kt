package com.github.butvinmitmo.notificationservice.config

import com.github.butvinmitmo.notificationservice.security.HeaderAuthenticationWebFilter
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
    private val headerAuthenticationWebFilter: HeaderAuthenticationWebFilter,
) {
    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain =
        http
            .csrf { it.disable() }
            .authorizeExchange { auth ->
                auth
                    .pathMatchers("/actuator/**")
                    .permitAll()
                    .pathMatchers("/swagger-ui/**", "/v3/api-docs/**", "/webjars/**")
                    .permitAll()
                    .anyExchange()
                    .permitAll()
            }.addFilterAt(headerAuthenticationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            .build()
}
