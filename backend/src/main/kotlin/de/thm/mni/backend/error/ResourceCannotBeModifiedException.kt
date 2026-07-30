package de.thm.mni.backend.error

/**
 * Signals that a resource state does not allow the requested modification.
 */
class ResourceCannotBeModifiedException (message: String): Exception(message)
