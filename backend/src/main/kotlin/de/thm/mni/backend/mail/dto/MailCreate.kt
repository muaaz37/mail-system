package de.thm.mni.backend.mail.dto

import de.thm.mni.backend.mail.enums.MailDeliveryMode
import java.util.UUID

/**
 * Service-layer payload for creating a new mail.
 */
data class MailCreate(
    override val subject: String,
    override val content: String,
    override val deliveryMode: MailDeliveryMode,
    override val toIds: MutableList<UUID>,
    override val ccIds: MutableList<UUID>,
    override val bccIds: MutableList<UUID>,
    override val replyToIds: MutableList<UUID>,
    override val externalTo: MutableList<String>,
    override val externalCc: MutableList<String>,
    override val externalBcc: MutableList<String>,
    override val externalReplyTo: MutableList<String>,
    override val replyToMailId: UUID?
) : MailPayload
