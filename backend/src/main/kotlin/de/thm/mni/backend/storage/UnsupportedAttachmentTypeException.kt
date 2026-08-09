package de.thm.mni.backend.storage

/** Signals that an uploaded file does not contain an allowed preview format. */
class UnsupportedAttachmentTypeException(message: String) : RuntimeException(message)
