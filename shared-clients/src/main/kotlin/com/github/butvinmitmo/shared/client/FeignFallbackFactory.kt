package com.github.butvinmitmo.shared.client

import feign.FeignException
import org.springframework.cloud.openfeign.FallbackFactory
import org.springframework.stereotype.Component
import java.lang.reflect.Proxy

/**
 * Generic fallback factory that creates a proxy for circuit breaker fallback.
 *
 * Behavior:
 * - 4xx client errors (404, 400, etc.) are re-thrown to propagate to the caller
 * - 5xx server errors, connection failures, and circuit open throw ServiceUnavailableException
 *
 * This ensures business errors (not found, bad request) are handled properly,
 * while infrastructure failures return 503 with the failed service name.
 */
@Component
class FeignFallbackFactory {
    fun <T> create(
        serviceName: String,
        clientClass: Class<T>,
        cause: Throwable,
    ): T {
        @Suppress("UNCHECKED_CAST")
        return Proxy.newProxyInstance(
            clientClass.classLoader,
            arrayOf(clientClass),
        ) { _, _, _ ->
            // Re-throw 4xx client errors - these are business errors, not infrastructure failures
            if (cause is FeignException && cause.status() in 400..499) {
                throw cause
            }
            // For 5xx, connection errors, timeouts - throw ServiceUnavailableException
            throw ServiceUnavailableException(serviceName, cause)
        } as T
    }
}

@Component
class UserServiceFallbackFactory(
    private val factory: FeignFallbackFactory,
) : FallbackFactory<UserServiceClient> {
    override fun create(cause: Throwable): UserServiceClient =
        factory.create("user-service", UserServiceClient::class.java, cause)
}

@Component
class TarotServiceFallbackFactory(
    private val factory: FeignFallbackFactory,
) : FallbackFactory<TarotServiceClient> {
    override fun create(cause: Throwable): TarotServiceClient =
        factory.create("tarot-service", TarotServiceClient::class.java, cause)
}

@Component
class DivinationServiceInternalFallbackFactory(
    private val factory: FeignFallbackFactory,
) : FallbackFactory<DivinationServiceInternalClient> {
    override fun create(cause: Throwable): DivinationServiceInternalClient =
        factory.create("divination-service", DivinationServiceInternalClient::class.java, cause)
}

@Component
class FilesServiceInternalFallbackFactory(
    private val factory: FeignFallbackFactory,
) : FallbackFactory<FilesServiceInternalClient> {
    override fun create(cause: Throwable): FilesServiceInternalClient =
        factory.create("files-service", FilesServiceInternalClient::class.java, cause)
}
