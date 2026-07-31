package de.thm.mni.backend.openapi

import de.thm.mni.backend.error.AppError
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType

const val BEARER_AUTH_SCHEME = "bearerAuth"

/**
 * Keeps Swagger UI resource groups in the order in which API consumers use them.
 */
@Configuration
class OpenApiConfiguration {
    @Bean
    fun orderedOpenApiTags(): OpenApiCustomizer = OpenApiCustomizer { openApi ->
        openApi.tags = openApi.tags?.sortedBy { tag ->
            TAG_ORDER.indexOf(tag.name).takeIf { index -> index >= 0 } ?: Int.MAX_VALUE
        }
    }

    /**
     * Defines the order in which API tags are displayed in Swagger UI.
     */
    private companion object {
        val TAG_ORDER = listOf("Auth", "User", "Mail", "Support Ticket", "Attachment", "IMAP Diagnostics")
    }
}

/**
 * Declares JWT bearer authentication and its standard authentication error.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@SecurityRequirement(name = BEARER_AUTH_SCHEME)
@ApiResponse(
    responseCode = "401",
    description = "Authentication is missing, expired, or invalid.",
    content = [Content(
        mediaType = MediaType.APPLICATION_JSON_VALUE,
        schema = Schema(implementation = AppError::class)
    )]
)
annotation class BearerAuthenticated

/**
 * Documents the error response for unexpected internal server errors (HTTP 500).
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@ApiResponse(
    responseCode = "500",
    description = "An unexpected internal server error occurred.",
    content = [Content(
        mediaType = MediaType.APPLICATION_JSON_VALUE,
        schema = Schema(implementation = AppError::class)
    )]
)
annotation class DefaultApiErrors

/**
 * Documents the error response for bad requests (HTTP 400).
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@ApiResponse(
    responseCode = "400", description = "The request is invalid.",
    content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = AppError::class))]
)
annotation class BadRequestApiResponse

/**
 * Documents the error response for unauthorized requests (HTTP 401).
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@ApiResponse(
    responseCode = "401", description = "Authentication credentials are missing or invalid.",
    content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = AppError::class))]
)
annotation class UnauthorizedApiResponse

/**
 * Documents the error response for forbidden requests (HTTP 403).
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@ApiResponse(
    responseCode = "404", description = "The requested resource was not found or is not visible to the user.",
    content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = AppError::class))]
)
annotation class NotFoundApiResponse

/**
 * Documents the error response for conflict requests (HTTP 409).
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@ApiResponse(
    responseCode = "409", description = "The request conflicts with the current resource state.",
    content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = AppError::class))]
)
annotation class ConflictApiResponse

/**
 * Documents the error response for payload too large requests (HTTP 413).
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@ApiResponse(
    responseCode = "413", description = "An uploaded file or the complete request exceeds the configured size limit.",
    content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = AppError::class))]
)
annotation class PayloadTooLargeApiResponse

/**
 * Documents the error response for bad gateway requests (HTTP 502).
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@ApiResponse(
    responseCode = "502", description = "An external mail or storage service is unavailable.",
    content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = AppError::class))]
)
annotation class BadGatewayApiResponse
