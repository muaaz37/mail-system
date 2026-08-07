package de.thm.mni.backend.mail.dto

import de.thm.mni.backend.attachment.dto.AttachmentDTO
import de.thm.mni.backend.mail.enums.MailDeliveryMode
import de.thm.mni.backend.mail.enums.MailSource
import de.thm.mni.backend.mail.enums.MailStatus
import de.thm.mni.backend.ticket.enums.SupportTicketPriority
import de.thm.mni.backend.ticket.enums.SupportTicketStatus
import de.thm.mni.backend.user.dto.UserDTO
import java.time.LocalDateTime
import java.util.UUID

/**
 * API response model for mails shown in inbox, sent and draft views.
 */
data class MailDTO(
    val id: UUID?,
    val replyToMailId: UUID?,
    val sender: UserDTO?,
    val externalSenderEmail: String?,
    val externalSenderName: String?,
    val externalMessageId: String?,
    val externalSentAt: LocalDateTime?,
    val ticketNumber: String?,
    val ticketId: UUID?,
    val ticketStatus: SupportTicketStatus?,
    val ticketPriority: SupportTicketPriority?,
    val ticketAssignedTo: UserDTO?,
    val subject: String,
    val content: String,
    val status: MailStatus,
    val source: MailSource,
    val deliveryMode: MailDeliveryMode,
    val isRead: Boolean,
    val to: List<UserDTO>,
    val cc: List<UserDTO>,
    val bcc: List<UserDTO>,
    val replyTo: List<UserDTO>,
    val externalTo: List<String>,
    val externalCc: List<String>,
    val externalBcc: List<String>,
    val externalReplyTo: List<String>,
    val attachments: List<AttachmentDTO>,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val sentAt: LocalDateTime?,
)
