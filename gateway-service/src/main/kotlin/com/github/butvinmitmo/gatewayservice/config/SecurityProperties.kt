package com.github.butvinmitmo.gatewayservice.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "security")
data class SecurityProperties(
    val publicPaths: List<String> = emptyList(),
)
