package com.github.butvinmitmo.userservice.exception

class NotFoundException(
    message: String,
) : RuntimeException(message)

class ConflictException(
    message: String,
) : RuntimeException(message)
