package com.github.butvinmitmo.shared.client

/**
 * Exception thrown when a downstream service is unavailable.
 * Used by circuit breaker fallbacks to indicate which service failed.
 */
class ServiceUnavailableException(
    val serviceName: String,
    cause: Throwable? = null,
) : RuntimeException("Service '$serviceName' is temporarily unavailable", cause)
