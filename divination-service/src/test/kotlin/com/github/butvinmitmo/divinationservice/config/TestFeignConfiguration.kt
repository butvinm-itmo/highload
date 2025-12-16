package com.github.butvinmitmo.divinationservice.config

import org.springframework.boot.autoconfigure.http.HttpMessageConverters
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter

@TestConfiguration
class TestFeignConfiguration {
    @Bean
    fun httpMessageConverters(): HttpMessageConverters = HttpMessageConverters(MappingJackson2HttpMessageConverter())
}
