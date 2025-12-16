package com.github.butvinmitmo.divinationservice.config

import org.springframework.boot.autoconfigure.http.HttpMessageConverters
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter

/**
 * Configuration for Feign clients in WebFlux environment.
 *
 * Spring WebFlux doesn't provide HttpMessageConverters autoconfiguration
 * (unlike Spring MVC), but Feign clients need them for JSON serialization.
 * This configuration manually provides the required bean.
 */
@Configuration
class FeignConfiguration {
    @Bean
    fun httpMessageConverters(): HttpMessageConverters = HttpMessageConverters(MappingJackson2HttpMessageConverter())
}
