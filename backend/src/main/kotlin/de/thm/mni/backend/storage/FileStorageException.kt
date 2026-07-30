package de.thm.mni.backend.storage

/**
 * Signals failures while storing, loading or deleting files.
 */
class FileStorageException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/**
 * Signals that the requested attachment object no longer exists in object storage.
 */
class FileStorageObjectNotFoundException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
