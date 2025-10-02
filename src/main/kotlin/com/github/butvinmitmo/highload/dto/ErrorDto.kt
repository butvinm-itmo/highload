package com.github.butvinmitmo.highload.dto

import java.time.Instant

data class ErrorResponse(
    val error: String,
    val message: String,
    val timestamp: Instant = Instant.now(),
    val path: String? = null,
)

data class ValidationErrorResponse(
    val error: String = "VALIDATION_ERROR",
    val message: String,
    val timestamp: Instant = Instant.now(),
    val path: String? = null,
    val fieldErrors: Map<String, String> = emptyMap(),
)