package de.thm.mni.backend.error

/**
 * Standard error response body returned by REST exception handlers.
 */
class AppError {
    val status: Int
    val message: String?

    constructor(status: Int, message: String?) {
        this.status = status
        this.message = message
    }
}
