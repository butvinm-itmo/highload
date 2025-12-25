package com.github.butvinmitmo.divinationservice.exception

class NotFoundException(
    message: String,
) : RuntimeException(message)

class ForbiddenException(
    message: String,
) : RuntimeException(message)

class ConflictException(
    message: String,
) : RuntimeException(message)

class InvalidFileException(
    message: String,
) : RuntimeException(message)
