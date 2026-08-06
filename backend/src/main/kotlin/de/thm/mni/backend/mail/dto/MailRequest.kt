package de.thm.mni.backend.mail.dto

import de.thm.mni.backend.mail.enums.MailDeliveryMode
import de.thm.mni.backend.mail.validation.AtLeastOneRecipient
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import java.util.UUID

/**
 * Multipart request payload used by the mail API for create and update operations.
 */
@AtLeastOneRecipient
@Schema(description = "Mail data supplied as the JSON part named `data` in multipart create and update requests.")
data class MailRequest(
    @field:Schema(description = "Mail subject.", example = "Unable to access my account")
    @field:Size(min = 1, max = 500, message = "Subject must be between 1 and 500 characters")
    val subject: String,
    @field:Schema(
        description = "Plain-text mail body.",
        example = "Hello, I cannot access my account. Could you please help me?"
    )
    @field:Size(min = 1, max = 10000, message = "Content must be between 1 and 10000 characters")
    val content: String,
    @field:Schema(
        description = "Selects internal application delivery or external SMTP delivery.",
        example = "EXTERNAL"
    )
    val deliveryMode: MailDeliveryMode = MailDeliveryMode.INTERNAL,
    @field:Schema(
        description = "Local user IDs returned by `GET /api/users`. " +
            "Do not use an OpenID Connect `sub` value."
    )
    val toIds: MutableList<UUID> = mutableListOf(),
    @field:Schema(description = "Local user IDs returned by `GET /api/users` for the Cc field.")
    val ccIds: MutableList<UUID> = mutableListOf(),
    @field:Schema(description = "Local user IDs returned by `GET /api/users` for the Bcc field.")
    val bccIds: MutableList<UUID> = mutableListOf(),
    @field:Schema(description = "Local user IDs returned by `GET /api/users` for the Reply-To field.")
    val replyToIds: MutableList<UUID> = mutableListOf(),
    @field:Schema(description = "External email addresses in the To field.", example = "[\"customer@example.com\"]")
    val externalTo: MutableList<String> = mutableListOf(),
    @field:Schema(description = "External email addresses in the Cc field.")
    val externalCc: MutableList<String> = mutableListOf(),
    @field:Schema(description = "External email addresses in the Bcc field.")
    val externalBcc: MutableList<String> = mutableListOf(),
    @field:Schema(description = "External email addresses in the Reply-To field.")
    val externalReplyTo: MutableList<String> = mutableListOf(),
    @field:Schema(description = "Original mail identifier when this mail is a reply. Obtain it from a mail response.")
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
