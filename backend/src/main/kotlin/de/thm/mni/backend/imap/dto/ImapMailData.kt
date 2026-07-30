package de.thm.mni.backend.imap.dto

import java.util.Date

/**
 * Full import payload extracted from an unread IMAP message.
 */
data class ImapMailData(
    val subject: String,
    val from: String?,
    val to: List<String>,
    val cc: List<String>,
    val replyTo: List<String>,
    val sentDate: Date?,
    val body: String?,
    val messageId: String?,
    val systemGenerated: Boolean,
    val attachments: List<ImapMailAttachment>
)
