package de.thm.mni.backend.mail.dto

import de.thm.mni.backend.mail.enums.MailDeliveryMode
import java.util.UUID

/**
 * Shared mail payload fields used by create and update use cases.
 */
interface MailPayload {
    val subject: String
    val content: String
    val deliveryMode: MailDeliveryMode
    val toIds: MutableList<UUID>
    val ccIds: MutableList<UUID>
    val bccIds: MutableList<UUID>
    val replyToIds: MutableList<UUID>
    val externalTo: MutableList<String>
    val externalCc: MutableList<String>
    val externalBcc: MutableList<String>
    val externalReplyTo: MutableList<String>
    val replyToMailId: UUID?
}
