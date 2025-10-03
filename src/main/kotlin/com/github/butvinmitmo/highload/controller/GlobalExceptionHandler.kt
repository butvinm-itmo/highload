package com.github.butvinmitmo.highload.controller

import com.github.butvinmitmo.highload.dto.ErrorResponse
import com.github.butvinmitmo.highload.dto.ValidationErrorResponse
import com.github.butvinmitmo.highload.exception.ConflictException
import com.github.butvinmitmo.highload.exception.ForbiddenException
import com.github.butvinmitmo.highload.exception.NotFoundException
import com.github.butvinmitmo.highload.exception.ValidationException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(NotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNotFound(ex: NotFoundException): ErrorResponse =
        ErrorResponse(
            error = "NOT_FOUND",
            message = ex.message ?: "Resource not found",
            timestamp = Instant.now(),
        )

    @ExceptionHandler(ForbiddenException::class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    fun handleForbidden(ex: ForbiddenException): ErrorResponse =
        ErrorResponse(
            error = "FORBIDDEN",
            message = ex.message ?: "Access denied",
            timestamp = Instant.now(),
        )

    @ExceptionHandler(ConflictException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleConflict(ex: ConflictException): ErrorResponse =
        ErrorResponse(
            error = "CONFLICT",
            message = ex.message ?: "Resource conflict",
            timestamp = Instant.now(),
        )

    @ExceptionHandler(ValidationException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleValidation(ex: ValidationException): ValidationErrorResponse =
        ValidationErrorResponse(
            error = "VALIDATION_ERROR",
            message = "Validation failed",
            timestamp = Instant.now(),
            fieldErrors = ex.errors,
        )

    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleIllegalArgument(ex: IllegalArgumentException): ErrorResponse =
        ErrorResponse(
            error = "BAD_REQUEST",
            message = ex.message ?: "Invalid request",
            timestamp = Instant.now(),
        )

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleGenericException(ex: Exception): ErrorResponse =
        ErrorResponse(
            error = "INTERNAL_SERVER_ERROR",
            message = "An unexpected error occurred",
            timestamp = Instant.now(),
        )
}
