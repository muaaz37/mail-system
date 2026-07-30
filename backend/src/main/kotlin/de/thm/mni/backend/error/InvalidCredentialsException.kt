package de.thm.mni.backend.error

/**
 * Signals invalid authentication credentials.
 */
class InvalidCredentialsException(message: String) : Exception(message)
