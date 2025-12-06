package com.github.butvinmitmo.e2e.config

import feign.codec.ErrorDecoder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FeignConfig {
    @Bean
    fun errorDecoder(): ErrorDecoder = ErrorDecoder.Default()
}
