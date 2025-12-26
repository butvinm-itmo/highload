package com.github.butvinmitmo.userservice.exception

import com.github.butvinmitmo.shared.dto.ErrorResponse
import com.github.butvinmitmo.shared.dto.ValidationErrorResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import java.time.Instant

@RestControllerAdvice
class GlobalExceptionHandler {
    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleValidationExceptions(
        ex: MethodArgumentNotValidException,
        request: WebRequest,
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
                path = request.getDescription(false).removePrefix("uri="),
                fieldErrors = errors,
            )

        return ResponseEntity(response, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(NotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNotFoundException(
        ex: NotFoundException,
        request: WebRequest,
    ): ResponseEntity<ErrorResponse> {
        val response =
            ErrorResponse(
                error = "NOT_FOUND",
                message = ex.message ?: "Resource not found",
                timestamp = Instant.now(),
                path = request.getDescription(false).removePrefix("uri="),
            )
        return ResponseEntity(response, HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(ConflictException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleConflictException(
        ex: ConflictException,
        request: WebRequest,
    ): ResponseEntity<ErrorResponse> {
        val response =
            ErrorResponse(
                error = "CONFLICT",
                message = ex.message ?: "Conflict occurred",
                timestamp = Instant.now(),
                path = request.getDescription(false).removePrefix("uri="),
            )
        return ResponseEntity(response, HttpStatus.CONFLICT)
    }

    @ExceptionHandler(UnauthorizedException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun handleUnauthorizedException(
        ex: UnauthorizedException,
        request: WebRequest,
    ): ResponseEntity<ErrorResponse> {
        val response =
            ErrorResponse(
                error = "UNAUTHORIZED",
                message = ex.message ?: "Unauthorized",
                timestamp = Instant.now(),
                path = request.getDescription(false).removePrefix("uri="),
            )
        return ResponseEntity(response, HttpStatus.UNAUTHORIZED)
    }

    @ExceptionHandler(ForbiddenException::class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    fun handleForbiddenException(
        ex: ForbiddenException,
        request: WebRequest,
    ): ResponseEntity<ErrorResponse> {
        val response =
            ErrorResponse(
                error = "FORBIDDEN",
                message = ex.message ?: "Forbidden",
                timestamp = Instant.now(),
                path = request.getDescription(false).removePrefix("uri="),
            )
        return ResponseEntity(response, HttpStatus.FORBIDDEN)
    }

    @ExceptionHandler(AccessDeniedException::class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    fun handleAccessDeniedException(
        ex: AccessDeniedException,
        request: WebRequest,
    ): ResponseEntity<ErrorResponse> {
        val response =
            ErrorResponse(
                error = "FORBIDDEN",
                message = ex.message ?: "Access denied",
                timestamp = Instant.now(),
                path = request.getDescription(false).removePrefix("uri="),
            )
        return ResponseEntity(response, HttpStatus.FORBIDDEN)
    }

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleGenericException(
        ex: Exception,
        request: WebRequest,
    ): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected error", ex)
        val response =
            ErrorResponse(
                error = "INTERNAL_SERVER_ERROR",
                message = "An unexpected error occurred",
                timestamp = Instant.now(),
                path = request.getDescription(false).removePrefix("uri="),
            )
        return ResponseEntity(response, HttpStatus.INTERNAL_SERVER_ERROR)
    }
}
