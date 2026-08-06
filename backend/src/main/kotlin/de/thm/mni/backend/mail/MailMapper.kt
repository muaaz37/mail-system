package de.thm.mni.backend.mail

import de.thm.mni.backend.attachment.dto.toDTO
import de.thm.mni.backend.mail.dto.MailDTO
import de.thm.mni.backend.mail.enums.MailDeliveryMode
import de.thm.mni.backend.mail.enums.MailSource
import de.thm.mni.backend.mail.enums.MailType
import de.thm.mni.backend.mailrecord.MailRecordService
import de.thm.mni.backend.user.User
import de.thm.mni.backend.user.dto.toDTO
import org.springframework.stereotype.Component

/**
 * Maps mail entities to API DTOs with user-specific visibility rules.
 */
@Component
class MailMapper(private val mailRecordService: MailRecordService) {
    /**
     * Converts a mail to the response model and hides BCC recipients where necessary.
     */
    fun toDTO(user: User, mail: Mail): MailDTO {
        val records = mailRecordService.getMailRecordByMailId(mail.id!!)
        val isCurrentUserSender = mail.sender?.id == user.id

        return MailDTO(
            id = mail.id,
            sender = mail.sender?.toDTO(),
            externalSenderEmail = mail.externalSenderEmail,
            externalSenderName = mail.externalSenderName,
            externalMessageId = mail.externalMessageId,
            externalSentAt = mail.externalSentAt,
            ticketNumber = mail.ticketNumber,
            ticketId = mail.ticket?.id,
            ticketStatus = mail.ticket?.status,
            ticketPriority = mail.ticket?.priority,
            ticketAssignedTo = mail.ticket?.assignedTo?.toDTO(),
            subject = mail.subject,
            content = mail.content,
            status = mail.status,
            source = if (mail.sender == null) MailSource.EXTERN else MailSource.INTERN,
            deliveryMode = mail.deliveryMode,
            to = records.filter { record -> record.type == MailType.TO }.map { record -> record.user!!.toDTO() },
            cc = records.filter { record -> record.type == MailType.CC }.map { record -> record.user!!.toDTO() },
            bcc = records
                .filter { record -> record.type == MailType.BCC }
                .filter { record -> record.user!!.id == user.id || isCurrentUserSender }
                .map { record -> record.user!!.toDTO() },
            replyTo = records
                .filter { record -> record.type == MailType.REPLY_TO }
                .map { record -> record.user!!.toDTO() },
            externalTo = mail.externalTo.toRecipientList(),
            externalCc = mail.externalCc.toRecipientList(),
            externalBcc = if (isCurrentUserSender) mail.externalBcc.toRecipientList() else emptyList(),
            externalReplyTo = mail.externalReplyTo.toRecipientList(),
            attachments = mail.attachments.map { attachment -> attachment.toDTO() },
            createdAt = mail.createdAt,
            updatedAt = mail.updatedAt,
            sentAt = mail.sentAt
        )
    }
}

/**
 * Converts the stored comma-separated recipient string to a list.
 * Null is accepted for compatibility with rows created before recipient defaults existed.
 */
fun String?.toRecipientList(): List<String> {
    return this.orEmpty().split(RECIPIENT_SEPARATOR)
        .map { recipient -> recipient.trim() }
        .filter { recipient -> recipient.isNotBlank() }
}

/**
 * Normalizes a recipient list for storage and removes duplicates case-insensitively.
 */
fun List<String>.toRecipientString(): String {
    return map { recipient -> recipient.trim() }
        .filter { recipient -> recipient.isNotBlank() }
        .distinctBy { recipient -> recipient.lowercase() }
        .joinToString(RECIPIENT_SEPARATOR)
}

private const val RECIPIENT_SEPARATOR = ","
