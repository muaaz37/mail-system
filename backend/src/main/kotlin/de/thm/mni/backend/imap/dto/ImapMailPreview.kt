package de.thm.mni.backend.imap.dto

import java.util.Date

/**
 * Lightweight diagnostic view of an unread IMAP message.
 */
data class ImapMailPreview(
    val subject: String,
    val from: String?,
    val sentDate: Date?,
    val body: String?,
    val messageId: String?
)
