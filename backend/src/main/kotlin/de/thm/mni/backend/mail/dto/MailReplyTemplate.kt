package de.thm.mni.backend.mail.dto

import de.thm.mni.backend.mail.enums.MailDeliveryMode
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

/**
 * Base interface for all mail reply templates.
 */
@Schema(
    description = "Prefilled data for replying to an internal or external mail.",
    oneOf = [
        InternalMailReplyTemplate::class,
        ExternalMailReplyTemplate::class
    ],
    discriminatorProperty = "deliveryMode"
)
sealed interface MailReplyTemplate {
    val replyToMailId: UUID
    val deliveryMode: MailDeliveryMode
    val subject: String
}

/**
 * Prefilled data for replying to an internal application message.
 */
@Schema(description = "Prefilled data for an internal mail reply.")
data class InternalMailReplyTemplate(
    override val replyToMailId: UUID,
    override val subject: String,
    val recipientIds: List<UUID>,
) : MailReplyTemplate {
    override val deliveryMode: MailDeliveryMode = MailDeliveryMode.INTERNAL
}

/**
 * Prefilled data for replying to an external support message.
 */
@Schema(description = "Prefilled data for an external support reply.")
data class ExternalMailReplyTemplate(
    override val replyToMailId: UUID,
    override val subject: String,
    val ticketNumber: String,
    val recipients: List<String>,
) : MailReplyTemplate {
    override val deliveryMode: MailDeliveryMode = MailDeliveryMode.EXTERNAL
}
