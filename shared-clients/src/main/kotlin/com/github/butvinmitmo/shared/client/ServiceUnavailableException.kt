package com.github.butvinmitmo.shared.client

class ServiceUnavailableException(
    val serviceName: String,
    cause: Throwable? = null,
) : RuntimeException("Service '$serviceName' is temporarily unavailable", cause)
