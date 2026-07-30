package de.thm.mni.backend.error

/**
 * Signals that a requested resource cannot be created because it already exists.
 */
class ResourceAlreadyExistsException(message: String) : Exception(message)
