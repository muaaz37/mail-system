package de.thm.mni.backend.error

/**
 * Signals that a resource is missing or not visible to the current user.
 */
class ResourceNotFoundException(message: String): Exception(message)
