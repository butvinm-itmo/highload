package com.github.butvinmitmo.e2e.config

import feign.Client
import feign.RequestInterceptor
import feign.RequestTemplate
import feign.hc5.ApacheHttp5Client
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Thread-local storage for JWT tokens in E2E tests.
 * Uses InheritableThreadLocal so Awaitility threads can access the token.
 */
object AuthContext {
    private val tokenHolder = InheritableThreadLocal<String>()

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

    /**
     * Use Apache HttpClient5 which supports PATCH method
     */
    @Bean
    fun feignClient(): Client {
        val httpClient = HttpClients.createDefault()
        return ApacheHttp5Client(httpClient)
    }
}
