package de.thm.mni.backend.error

/**
 * Signals invalid mail data or unsupported mail operations.
 */
class InvalidMailRequestException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
