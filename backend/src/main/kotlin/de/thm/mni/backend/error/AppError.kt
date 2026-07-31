package de.thm.mni.backend.error

import io.swagger.v3.oas.annotations.media.Schema

/**
 * Standard error response body returned by REST exception handlers.
 */
@Schema(description = "Standard error response returned by the API.")
class AppError {
    @field:Schema(description = "HTTP status code.", example = "400")
    val status: Int
    @field:Schema(description = "Human-readable explanation of the error.", example = "Invalid request body.")
    val message: String?

    constructor(status: Int, message: String?) {
        this.status = status
        this.message = message
    }
}
