package com.github.butvinmitmo.highload.exception

class NotFoundException(
    message: String,
) : RuntimeException(message)

class ForbiddenException(
    message: String,
) : RuntimeException(message)

class ConflictException(
    message: String,
) : RuntimeException(message)

class ValidationException(
    val errors: Map<String, String>,
) : RuntimeException()
