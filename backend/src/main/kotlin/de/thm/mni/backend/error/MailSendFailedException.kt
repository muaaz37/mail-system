package de.thm.mni.backend.error

/**
 * Raised when an outgoing mail could not be delivered through the configured transport.
 */
class MailSendFailedException(message: String) : Exception(message)
