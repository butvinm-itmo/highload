package com.github.butvinmitmo.divinationservice.exception

import com.github.butvinmitmo.shared.dto.ErrorResponse
import com.github.butvinmitmo.shared.dto.ValidationErrorResponse
import feign.FeignException
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import org.slf4j.LoggerFactory
import org.springframework.cloud.client.circuitbreaker.NoFallbackAvailableException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.bind.support.WebExchangeBindException
import org.springframework.web.server.ServerWebExchange
import java.time.Instant

@RestControllerAdvice
class GlobalExceptionHandler {
    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(WebExchangeBindException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleValidationExceptions(
        ex: WebExchangeBindException,
        exchange: ServerWebExchange,
    ): ResponseEntity<ValidationErrorResponse> {
        val errors = mutableMapOf<String, String>()

        ex.bindingResult.allErrors.forEach { error ->
            val fieldName = (error as? FieldError)?.field ?: "unknown"
            val errorMessage = error.defaultMessage ?: "Invalid value"
            errors[fieldName] = errorMessage
        }

        val response =
            ValidationErrorResponse(
                error = "VALIDATION_ERROR",
                message = "Validation failed",
                timestamp = Instant.now(),
                path = exchange.request.path.value(),
                fieldErrors = errors,
            )

        return ResponseEntity(response, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(NotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNotFoundException(
        ex: NotFoundException,
        exchange: ServerWebExchange,
    ): ResponseEntity<ErrorResponse> {
        val response =
            ErrorResponse(
                error = "NOT_FOUND",
                message = ex.message ?: "Resource not found",
                timestamp = Instant.now(),
                path = exchange.request.path.value(),
            )
        return ResponseEntity(response, HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(ConflictException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleConflictException(
        ex: ConflictException,
        exchange: ServerWebExchange,
    ): ResponseEntity<ErrorResponse> {
        val response =
            ErrorResponse(
                error = "CONFLICT",
                message = ex.message ?: "Conflict occurred",
                timestamp = Instant.now(),
                path = exchange.request.path.value(),
            )
        return ResponseEntity(response, HttpStatus.CONFLICT)
    }

    @ExceptionHandler(ForbiddenException::class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    fun handleForbiddenException(
        ex: ForbiddenException,
        exchange: ServerWebExchange,
    ): ResponseEntity<ErrorResponse> {
        val response =
            ErrorResponse(
                error = "FORBIDDEN",
                message = ex.message ?: "Access forbidden",
                timestamp = Instant.now(),
                path = exchange.request.path.value(),
            )
        return ResponseEntity(response, HttpStatus.FORBIDDEN)
    }

    @ExceptionHandler(InvalidFileException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleInvalidFileException(
        ex: InvalidFileException,
        exchange: ServerWebExchange,
    ): ResponseEntity<ErrorResponse> {
        val response =
            ErrorResponse(
                error = "INVALID_FILE",
                message = ex.message ?: "Invalid file",
                timestamp = Instant.now(),
                path = exchange.request.path.value(),
            )
        return ResponseEntity(response, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(FeignException.NotFound::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleFeignNotFoundException(
        ex: FeignException.NotFound,
        exchange: ServerWebExchange,
    ): ResponseEntity<ErrorResponse> {
        val response =
            ErrorResponse(
                error = "NOT_FOUND",
                message = "Referenced resource not found",
                timestamp = Instant.now(),
                path = exchange.request.path.value(),
            )
        return ResponseEntity(response, HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(NoFallbackAvailableException::class)
    fun handleNoFallbackAvailableException(
        ex: NoFallbackAvailableException,
        exchange: ServerWebExchange,
    ): ResponseEntity<ErrorResponse> {
        // Unwrap the cause and delegate to appropriate handler
        val cause = ex.cause
        return when (cause) {
            is FeignException.NotFound -> handleFeignNotFoundException(cause, exchange)
            is FeignException -> handleFeignException(cause, exchange)
            else -> handleGenericException(ex, exchange)
        }
    }

    @ExceptionHandler(CallNotPermittedException::class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    fun handleCircuitBreakerOpenException(
        ex: CallNotPermittedException,
        exchange: ServerWebExchange,
    ): ResponseEntity<ErrorResponse> {
        val response =
            ErrorResponse(
                error = "SERVICE_UNAVAILABLE",
                message = "Service temporarily unavailable. Please try again later.",
                timestamp = Instant.now(),
                path = exchange.request.path.value(),
            )
        return ResponseEntity(response, HttpStatus.SERVICE_UNAVAILABLE)
    }

    @ExceptionHandler(FeignException::class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    fun handleFeignException(
        ex: FeignException,
        exchange: ServerWebExchange,
    ): ResponseEntity<ErrorResponse> {
        val response =
            ErrorResponse(
                error = "BAD_GATEWAY",
                message = "Error communicating with downstream service",
                timestamp = Instant.now(),
                path = exchange.request.path.value(),
            )
        return ResponseEntity(response, HttpStatus.BAD_GATEWAY)
    }

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleGenericException(
        ex: Exception,
        exchange: ServerWebExchange,
    ): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected error", ex)
        val response =
            ErrorResponse(
                error = "INTERNAL_SERVER_ERROR",
                message = "An unexpected error occurred",
                timestamp = Instant.now(),
                path = exchange.request.path.value(),
            )
        return ResponseEntity(response, HttpStatus.INTERNAL_SERVER_ERROR)
    }
}
