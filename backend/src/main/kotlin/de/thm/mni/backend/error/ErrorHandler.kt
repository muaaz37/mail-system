package de.thm.mni.backend.error

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice


@RestControllerAdvice
class ErrorHandler {

    private val log = LoggerFactory.getLogger(ErrorHandler::class.java)

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleNotFoundException(err: ResourceNotFoundException): ResponseEntity<AppError> {
        log.error("Resource not found: ${err.message}")
        val error = AppError(HttpStatus.NOT_FOUND.value(), err.message)
        return ResponseEntity<AppError>(error, HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(InvalidCredentialsException::class)
    fun handleInvalidCredentialsException(err: InvalidCredentialsException): ResponseEntity<AppError> {
        log.error("Invalid credentials exception: ${err.message}")
        val error = AppError(HttpStatus.UNAUTHORIZED.value(), err.message)
        return ResponseEntity<AppError>(error, HttpStatus.UNAUTHORIZED)
    }

    @ExceptionHandler(ResourceAlreadyExistsException::class)
    fun handleResourceAlreadyExistsException(err: ResourceAlreadyExistsException): ResponseEntity<AppError> {
        log.error("Resource already exists: ${err.message}")
        val error = AppError(HttpStatus.CONFLICT.value(), err.message)
        return ResponseEntity<AppError>(error, HttpStatus.CONFLICT)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(err: MethodArgumentNotValidException): ResponseEntity<AppError> {
        val errorMessages = err.bindingResult.allErrors.joinToString("; ") { it.defaultMessage ?: "Invalid value" }
        log.error(errorMessages)
        val error = AppError(HttpStatus.BAD_REQUEST.value(), "Validation failed: $errorMessages")
        return ResponseEntity<AppError>(error, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(ResourceCannotBeModifiedException::class)
    fun handleResourceCannotBeModifiedException(err: ResourceCannotBeModifiedException): ResponseEntity<AppError> {
        log.error("Resource cannot be modified: ${err.message}")
        val error = AppError(HttpStatus.BAD_REQUEST.value(), err.message)
        return ResponseEntity<AppError>(error, HttpStatus.BAD_REQUEST)
    }

    // Handle all other exceptions
    @ExceptionHandler(Exception::class)
    fun handleAllExceptions(e: Exception): ResponseEntity<AppError> {
        log.error("An unexpected error occurred: ${e.message}", e)
        val error = AppError(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal server error: " + e.message
        )
        return ResponseEntity<AppError>(error, HttpStatus.INTERNAL_SERVER_ERROR)
    }


}