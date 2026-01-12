package com.github.butvinmitmo.userservice.exception

class NotFoundException(
    message: String,
) : RuntimeException(message)

class ConflictException(
    message: String,
) : RuntimeException(message)

class UnauthorizedException(
    message: String,
) : RuntimeException(message)

class ForbiddenException(
    message: String,
) : RuntimeException(message)

class ServiceUnavailableException(
    message: String,
) : RuntimeException(message)
