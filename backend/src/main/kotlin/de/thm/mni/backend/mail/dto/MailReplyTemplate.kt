package de.thm.mni.backend.mail.dto

import java.util.UUID

/**
 * Prefilled reply data returned before answering an incoming support mail.
 */
data class MailReplyTemplate(
    val replyToMailId: UUID,
    val ticketNumber: String,
    val subject: String,
    val externalTo: List<String>,
    val externalCc: List<String> = emptyList(),
    val externalBcc: List<String> = emptyList(),
    val externalReplyTo: List<String> = emptyList()
)
