package de.thm.mni.backend.mail.dto

import de.thm.mni.backend.mail.enums.MailDeliveryMode
import de.thm.mni.backend.mail.validation.AtLeastOneRecipient
import jakarta.validation.constraints.Size
import java.util.UUID

/**
 * Multipart request payload used by the mail API for create and update operations.
 */
@AtLeastOneRecipient
data class MailRequest(
    @field:Size(min = 1, max = 500, message = "Subject must be between 1 and 500 characters")
    val subject: String,
    @field:Size(min = 1, max = 10000, message = "Content must be between 1 and 10000 characters")
    val content: String,
    val deliveryMode: MailDeliveryMode = MailDeliveryMode.INTERNAL,
    val toIds: MutableList<UUID> = mutableListOf(),
    val ccIds: MutableList<UUID> = mutableListOf(),
    val bccIds: MutableList<UUID> = mutableListOf(),
    val replyToIds: MutableList<UUID> = mutableListOf(),
    val externalTo: MutableList<String> = mutableListOf(),
    val externalCc: MutableList<String> = mutableListOf(),
    val externalBcc: MutableList<String> = mutableListOf(),
    val externalReplyTo: MutableList<String> = mutableListOf(),
    val replyToMailId: UUID? = null
)

/**
 * Converts the API request into the create use-case payload.
 */
fun MailRequest.toMailCreate(): MailCreate {
    return MailCreate(
        subject = subject,
        content = content,
        deliveryMode = deliveryMode,
        toIds = toIds,
        ccIds = ccIds,
        bccIds = bccIds,
        replyToIds = replyToIds,
        externalTo = externalTo,
        externalCc = externalCc,
        externalBcc = externalBcc,
        externalReplyTo = externalReplyTo,
        replyToMailId = replyToMailId
    )
}

/**
 * Converts the API request into the update use-case payload.
 */
fun MailRequest.toMailUpdate(): MailUpdate {
    return MailUpdate(
        subject = subject,
        content = content,
        deliveryMode = deliveryMode,
        toIds = toIds,
        ccIds = ccIds,
        bccIds = bccIds,
        replyToIds = replyToIds,
        externalTo = externalTo,
        externalCc = externalCc,
        externalBcc = externalBcc,
        externalReplyTo = externalReplyTo,
        replyToMailId = replyToMailId
    )
}
