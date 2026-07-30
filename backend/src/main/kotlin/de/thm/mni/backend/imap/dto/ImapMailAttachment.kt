package de.thm.mni.backend.imap.dto

/**
 * Attachment content extracted from an incoming IMAP message.
 */
data class ImapMailAttachment(
    val fileName: String,
    val mimeType: String?,
    val bytes: ByteArray
)
