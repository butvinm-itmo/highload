package com.github.butvinmitmo.e2e.config

import feign.RequestInterceptor
import feign.RequestTemplate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Thread-local storage for JWT tokens in E2E tests
 */
object AuthContext {
    private val tokenHolder = ThreadLocal<String>()

    fun setToken(token: String?) {
        if (token == null) {
            tokenHolder.remove()
        } else {
            tokenHolder.set(token)
        }
    }

    fun getToken(): String? = tokenHolder.get()

    fun clear() {
        tokenHolder.remove()
    }
}

/**
 * Feign request interceptor that adds Authorization header from AuthContext
 */
@Configuration
class AuthFeignConfig {
    @Bean
    fun authRequestInterceptor(): RequestInterceptor =
        RequestInterceptor { template: RequestTemplate ->
            val token = AuthContext.getToken()
            if (token != null) {
                template.header("Authorization", "Bearer $token")
            }
        }
}
