package de.thm.mni.backend.mail.internal

import de.thm.mni.backend.error.InvalidMailRequestException
import de.thm.mni.backend.mail.Mail
import de.thm.mni.backend.mail.buildReplySubject
import de.thm.mni.backend.mail.dto.InternalMailReplyTemplate
import de.thm.mni.backend.mail.dto.MailPayload
import de.thm.mni.backend.mail.enums.MailDeliveryMode
import de.thm.mni.backend.mail.enums.MailStatus
import de.thm.mni.backend.mail.enums.MailType
import de.thm.mni.backend.mailrecord.MailRecordService
import de.thm.mni.backend.user.User
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Prepares and validates replies to mails delivered inside the application.
 */
@Service
class InternalReplyService(
    private val mailRecordService: MailRecordService
) {

    /**
     * Links an internal reply to its original mail without entering the
     * external support-ticket lifecycle.
     */
    fun applyReplyContext(
        replyMail: Mail,
        originalMail: Mail
    ) {
        if (replyMail.deliveryMode != MailDeliveryMode.INTERNAL) {
            throw InvalidMailRequestException(
                "Internal reply context requires internal delivery."
            )
        }

        ensureOriginalMailReplyable(originalMail)

        replyMail.inReplyToMail = originalMail
        replyMail.ticket = null
        replyMail.ticketNumber = null
    }

    /**
     * Builds a reply template for an internal mail received by the current
     * user.
     */
    fun getReplyTemplate(
        originalMail: Mail,
        currentUser: User
    ): InternalMailReplyTemplate {
        ensureReplyAllowed(originalMail, currentUser)

        return InternalMailReplyTemplate(
            replyToMailId = requireNotNull(originalMail.id),
            subject = buildReplySubject(originalMail.subject),
            recipientIds = resolveReplyRecipientIds(originalMail)
        )
    }

    /**
     * Replaces client-provided recipients with recipients derived from the
     * original mail.
     *
     * This prevents clients from changing the primary recipient while keeping
     * a foreign reply context.
     */
    fun enforceReplyContext(
        originalMail: Mail,
        payload: MailPayload,
        currentUser: User
    ) {
        ensureReplyAllowed(originalMail, currentUser)

        if (payload.deliveryMode != MailDeliveryMode.INTERNAL) {
            throw InvalidMailRequestException(
                "Replies to internal mails must use internal delivery."
            )
        }

        payload.toIds.clear()
        payload.toIds.addAll(resolveReplyRecipientIds(originalMail))
    }

    /**
     * Uses explicit internal Reply-To recipients when present and otherwise
     * falls back to the original sender.
     */
    private fun resolveReplyRecipientIds(originalMail: Mail): List<UUID> {
        val replyToIds = mailRecordService
            .getMailRecordByMailId(requireNotNull(originalMail.id))
            .filter { record -> record.type == MailType.REPLY_TO }
            .mapNotNull { record -> record.user?.id }
            .distinct()

        if (replyToIds.isNotEmpty()) {
            return replyToIds
        }

        return listOf(
            originalMail.sender?.id
                ?: throw InvalidMailRequestException(
                    "Internal mail has no valid sender."
                )
        )
    }

    /**
     * Verifies that the original mail is a sent internal mail that can be replied to.
     */
    private fun ensureOriginalMailReplyable(originalMail: Mail) {
        // Check if the original mail is an internal mail
        if (originalMail.deliveryMode != MailDeliveryMode.INTERNAL) {
            throw InvalidMailRequestException(
                "The selected mail is not an internal mail."
            )
        }

        // Check if the original mail is sent
        if (originalMail.status != MailStatus.SENT) {
            throw InvalidMailRequestException(
                "Only sent internal mails can be answered."
            )
        }

        // Check if the original mail has a sender
        if (originalMail.sender?.id == null) {
            throw InvalidMailRequestException(
                "Internal mail has no valid sender."
            )
        }
    }

    /**
     * Verifies that the current user is allowed to reply to the original mail.
     */
    private fun ensureReplyAllowed(
        originalMail: Mail,
        currentUser: User
    ) {
        ensureOriginalMailReplyable(originalMail)

        // Check if the current user is allowed to reply to the original mail
        if (originalMail.sender?.id == currentUser.id) {
            throw InvalidMailRequestException(
                "Users cannot reply to their own internal mail."
            )
        }
        // Check if the current user is a recipient of the original mail
        val isRecipient = mailRecordService
            .getMailRecordByMailId(requireNotNull(originalMail.id))
            .any { record ->
                record.type != MailType.REPLY_TO &&
                    record.user?.id == currentUser.id
            }

        if (!isRecipient) {
            throw InvalidMailRequestException(
                "The current user is not a recipient of this mail."
            )
        }
    }
}
