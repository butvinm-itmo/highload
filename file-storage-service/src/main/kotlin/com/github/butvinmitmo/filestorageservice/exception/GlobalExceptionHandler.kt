package com.github.butvinmitmo.filestorageservice.exception

import com.github.butvinmitmo.shared.dto.ErrorResponse
import com.github.butvinmitmo.shared.dto.ValidationErrorResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.multipart.MaxUploadSizeExceededException
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

    @ExceptionHandler(FileNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleFileNotFoundException(
        ex: FileNotFoundException,
        request: WebRequest,
    ): ResponseEntity<ErrorResponse> {
        val response =
            ErrorResponse(
                error = "NOT_FOUND",
                message = ex.message ?: "File not found",
                timestamp = Instant.now(),
                path = request.getDescription(false).removePrefix("uri="),
            )
        return ResponseEntity(response, HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(FileStorageException::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleFileStorageException(
        ex: FileStorageException,
        request: WebRequest,
    ): ResponseEntity<ErrorResponse> {
        logger.error("File storage error", ex)
        val response =
            ErrorResponse(
                error = "FILE_STORAGE_ERROR",
                message = ex.message ?: "File storage error occurred",
                timestamp = Instant.now(),
                path = request.getDescription(false).removePrefix("uri="),
            )
        return ResponseEntity(response, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @ExceptionHandler(MaxUploadSizeExceededException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleMaxUploadSizeExceededException(
        ex: MaxUploadSizeExceededException,
        request: WebRequest,
    ): ResponseEntity<ErrorResponse> {
        val response =
            ErrorResponse(
                error = "FILE_TOO_LARGE",
                message = "File size exceeds maximum allowed size",
                timestamp = Instant.now(),
                path = request.getDescription(false).removePrefix("uri="),
            )
        return ResponseEntity(response, HttpStatus.BAD_REQUEST)
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
