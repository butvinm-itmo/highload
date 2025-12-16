package com.github.butvinmitmo.shared.config

import feign.codec.ErrorDecoder
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SharedFeignConfig {
    @Bean
    @ConditionalOnMissingBean
    fun errorDecoder(): ErrorDecoder = ErrorDecoder.Default()
}
