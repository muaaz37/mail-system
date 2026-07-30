package de.thm.mni.backend.error

/**
 * Signals that a valid token points to a user that no longer exists in the database.
 */
class AuthenticatedUserNotFoundException(message: String) : Exception(message)
