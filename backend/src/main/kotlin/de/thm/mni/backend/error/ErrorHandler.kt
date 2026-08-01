package de.thm.mni.backend.error

import de.thm.mni.backend.storage.FileStorageException
import de.thm.mni.backend.storage.FileStorageObjectNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.multipart.MaxUploadSizeExceededException
import org.springframework.web.multipart.MultipartException

/**
 * Maps application exceptions to consistent HTTP error responses.
 */
@RestControllerAdvice
class ErrorHandler {
    private val log = LoggerFactory.getLogger(ErrorHandler::class.java)

    /**
     * Handles missing resources without revealing authorization details.
     */
    @ExceptionHandler(ResourceNotFoundException::class, FileStorageObjectNotFoundException::class)
    fun handleNotFoundException(err: Exception): ResponseEntity<AppError> {
        log.warn("Resource not found: {}", err.message)
        val message = when (err) {
            is FileStorageObjectNotFoundException -> ATTACHMENT_NOT_FOUND_MESSAGE
            else -> err.message
        }
        val error = AppError(HttpStatus.NOT_FOUND.value(), message)
        return ResponseEntity<AppError>(error, HttpStatus.NOT_FOUND)
    }

    /**
     * Handles stale JWT sessions after database recreation or user deletion.
     */
    @ExceptionHandler(AuthenticatedUserNotFoundException::class)
    fun handleAuthenticatedUserNotFoundException(
        err: AuthenticatedUserNotFoundException
    ): ResponseEntity<AppError> {
        log.warn("Authenticated user not found: {}", err.message)
        val error = AppError(HttpStatus.UNAUTHORIZED.value(), err.message)
        return ResponseEntity<AppError>(error, HttpStatus.UNAUTHORIZED)
    }

    /**
     * Handles conflicts such as duplicate email addresses.
     */
    @ExceptionHandler(ResourceAlreadyExistsException::class)
    fun handleResourceAlreadyExistsException(err: ResourceAlreadyExistsException): ResponseEntity<AppError> {
        log.warn("Resource already exists: {}", err.message)
        val error = AppError(HttpStatus.CONFLICT.value(), err.message)
        return ResponseEntity<AppError>(error, HttpStatus.CONFLICT)
    }

    /**
     * Handles bean validation errors from request payloads.
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(err: MethodArgumentNotValidException): ResponseEntity<AppError> {
        val errorMessages = err.bindingResult.allErrors.joinToString("; ") { it.defaultMessage ?: "Invalid value" }
        log.warn("Request validation failed: {}", errorMessages)
        val error = AppError(HttpStatus.BAD_REQUEST.value(), "Validation failed: $errorMessages")
        return ResponseEntity<AppError>(error, HttpStatus.BAD_REQUEST)
    }

    /**
     * Handles business-rule violations that should be corrected by the request.
     */
    @ExceptionHandler(
        ResourceCannotBeModifiedException::class,
        InvalidMailRequestException::class,
        MethodArgumentTypeMismatchException::class,
        HttpMessageNotReadableException::class
    )
    fun handleBadRequestException(err: Exception): ResponseEntity<AppError> {
        log.warn("Bad request: {}", err.message)
        val message = when (err) {
            is MethodArgumentTypeMismatchException -> INVALID_PATH_PARAMETER_MESSAGE
            is HttpMessageNotReadableException -> INVALID_REQUEST_BODY_MESSAGE
            else -> err.message
        }
        val error = AppError(HttpStatus.BAD_REQUEST.value(), message)
        return ResponseEntity<AppError>(error, HttpStatus.BAD_REQUEST)
    }

    /**
     * Converts oversized multipart uploads into a clear user-facing response.
     */
    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handleMaxUploadSizeExceededException(err: MaxUploadSizeExceededException): ResponseEntity<AppError> {
        log.warn("Upload size exceeded: {}", err.message)
        val error = AppError(HttpStatus.PAYLOAD_TOO_LARGE.value(), ATTACHMENT_TOO_LARGE_MESSAGE)
        return ResponseEntity<AppError>(error, HttpStatus.PAYLOAD_TOO_LARGE)
    }

    /**
     * Handles malformed multipart requests without exposing parser details.
     */
    @ExceptionHandler(MultipartException::class)
    fun handleMultipartException(err: MultipartException): ResponseEntity<AppError> {
        log.warn("Multipart upload could not be processed: {}", err.message)
        val error = AppError(HttpStatus.BAD_REQUEST.value(), ATTACHMENT_UPLOAD_FAILED_MESSAGE)
        return ResponseEntity<AppError>(error, HttpStatus.BAD_REQUEST)
    }

    /**
     * Reports multipart parts with an unsupported content type as a client error.
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException::class)
    fun handleUnsupportedMediaType(err: HttpMediaTypeNotSupportedException): ResponseEntity<AppError> {
        log.warn("Unsupported media type: {}", err.message)
        val error = AppError(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), UNSUPPORTED_MEDIA_TYPE_MESSAGE)
        return ResponseEntity<AppError>(error, HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    }

    /**
     * Handles SMTP or attachment failures while keeping the draft available for retry.
     */
    @ExceptionHandler(MailSendFailedException::class)
    fun handleMailSendFailedException(err: MailSendFailedException): ResponseEntity<AppError> {
        log.error("Mail send failed: {}", err.message, err)
        val error = AppError(HttpStatus.BAD_GATEWAY.value(), MAIL_SEND_FAILED_MESSAGE)
        return ResponseEntity<AppError>(error, HttpStatus.BAD_GATEWAY)
    }

    /**
     * Handles object-storage failures from the attachment layer.
     */
    @ExceptionHandler(FileStorageException::class)
    fun handleFileStorageException(err: FileStorageException): ResponseEntity<AppError> {
        log.error("Attachment storage failure: {}", err.message, err)
        val error = AppError(HttpStatus.BAD_GATEWAY.value(), ATTACHMENT_STORAGE_FAILED_MESSAGE)
        return ResponseEntity<AppError>(error, HttpStatus.BAD_GATEWAY)
    }

    /**
     * Handles unexpected errors with a generic 500 response.
     */
    @ExceptionHandler(Exception::class)
    fun handleAllExceptions(e: Exception): ResponseEntity<AppError> {
        log.error("An unexpected error occurred", e)
        val error = AppError(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            UNEXPECTED_ERROR_MESSAGE
        )
        return ResponseEntity<AppError>(error, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    private companion object {
        const val MAIL_SEND_FAILED_MESSAGE = "Mail could not be sent. The draft was kept for retry."
        const val INVALID_PATH_PARAMETER_MESSAGE = "Invalid request path."
        const val INVALID_REQUEST_BODY_MESSAGE = "Invalid request body."
        const val ATTACHMENT_TOO_LARGE_MESSAGE = "Attachment is too large. Maximum file size is 10 MB."
        const val ATTACHMENT_UPLOAD_FAILED_MESSAGE = "Attachment upload could not be processed."
        const val UNSUPPORTED_MEDIA_TYPE_MESSAGE = "The request contains an unsupported content type."
        const val ATTACHMENT_STORAGE_FAILED_MESSAGE = "Attachment storage is currently unavailable."
        const val ATTACHMENT_NOT_FOUND_MESSAGE = "Attachment was not found."
        const val UNEXPECTED_ERROR_MESSAGE = "Unexpected server error. Please try again later."
    }
}
