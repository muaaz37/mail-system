package de.thm.mni.backend.mail

import de.thm.mni.backend.mail.dto.MailPayload
import de.thm.mni.backend.mail.dto.MailReplyTemplate
import de.thm.mni.backend.mail.enums.MailDeliveryMode
import de.thm.mni.backend.user.User

/**
 * Handles reply rules for one mail delivery mode.
 */
interface MailReplyHandler {
    val deliveryMode: MailDeliveryMode

    fun applyReplyContext(replyMail: Mail, originalMail: Mail)

    fun getReplyTemplate(originalMail: Mail, currentUser: User): MailReplyTemplate

    fun enforceReplyContext(originalMail: Mail, payload: MailPayload, currentUser: User)
}
