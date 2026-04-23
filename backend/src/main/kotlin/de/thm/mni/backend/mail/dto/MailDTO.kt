package de.thm.mni.backend.mail.dto

import de.thm.mni.backend.attachment.dto.AttachmentDTO
import de.thm.mni.backend.mail.enums.MailSource
import de.thm.mni.backend.mail.enums.MailStatus
import de.thm.mni.backend.user.dto.UserDTO
import java.time.LocalDateTime
import java.util.UUID

data class MailDTO(
    val id: UUID?,
    val sender: UserDTO,
    val subject: String,
    val content: String,
    val status: MailStatus,
    val source: MailSource,
    val to: List<UserDTO>,
    val cc: List<UserDTO>,
    val bcc: List<UserDTO>,
    val replyTo: List<UserDTO>,
    val attachments: List<AttachmentDTO>,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val sentAt: LocalDateTime?,
)
