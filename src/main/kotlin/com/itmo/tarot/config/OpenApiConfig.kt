package com.itmo.tarot.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {
    
    @Bean
    fun customOpenAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("Tarot Service API")
                    .description("REST API for Tarot card reading web service")
                    .version("1.0.0")
                    .contact(
                        Contact()
                            .name("Tarot Service Team")
                            .email("support@tarot-service.com")
                    )
            )
            .servers(
                listOf(
                    Server()
                        .url("http://localhost:8080")
                        .description("Development server"),
                    Server()
                        .url("https://api.tarot-service.com")
                        .description("Production server")
                )
            )
    }
}