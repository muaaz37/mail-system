package de.thm.mni.backend.util.dto

import de.thm.mni.backend.mail.enums.MailStatus

data class CreateSeedMail(
    val senderEmail: String,
    val subject: String,
    val content: String,
    val status: MailStatus,
    val toEmails: List<String>,
    val ccEmails: List<String>,
    val bccEmails: List<String>,
    val replyToEmails: List<String>
)
