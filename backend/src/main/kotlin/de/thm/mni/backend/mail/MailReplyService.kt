package de.thm.mni.backend.mail

import de.thm.mni.backend.error.ResourceNotFoundException
import de.thm.mni.backend.mail.dto.MailPayload
import de.thm.mni.backend.mail.dto.MailReplyTemplate
import de.thm.mni.backend.mail.enums.MailDeliveryMode
import de.thm.mni.backend.user.User
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Coordinates reply preparation and delegates channel-specific validation.
 */
@Service
class MailReplyService(
    private val mailRepository: MailRepository,
    replyHandlers: List<MailReplyHandler>
) {
    private val replyHandlersByMode = replyHandlers.associateBy(MailReplyHandler::deliveryMode)

    init {
        require(replyHandlersByMode.size == replyHandlers.size) {
            "Only one mail reply handler may be configured per delivery mode."
        }
    }

    /**
     * Stores the relation to the original mail and applies channel-specific
     * conversation context.
     */
    @Transactional
    fun applyReplyContext(
        replyMail: Mail,
        replyToMailId: UUID?
    ) {
        if (replyToMailId == null) return

        val originalMail = findOriginalMail(replyToMailId)

        handlerFor(originalMail.deliveryMode).applyReplyContext(
            replyMail = replyMail,
            originalMail = originalMail
        )
    }

    /**
     * Creates a reply template for the original mail's delivery channel.
     */
    @Transactional
    fun getReplyTemplate(
        originalMail: Mail,
        currentUser: User
    ): MailReplyTemplate {
        return handlerFor(originalMail.deliveryMode).getReplyTemplate(originalMail, currentUser)
    }

    /**
     * Applies trusted reply recipients derived from the persisted original
     * mail.
     */
    @Transactional
    fun enforceReplyContext(
        payload: MailPayload,
        currentUser: User,
        storedOriginalMail: Mail? = null
    ) {
        val originalMail = payload.replyToMailId
            ?.let(::findOriginalMail)
            ?: storedOriginalMail
            ?: return

        handlerFor(originalMail.deliveryMode).enforceReplyContext(
            originalMail = originalMail,
            payload = payload,
            currentUser = currentUser
        )
    }

    private fun handlerFor(deliveryMode: MailDeliveryMode): MailReplyHandler {
        return replyHandlersByMode[deliveryMode]
            ?: error("No mail reply handler configured for delivery mode $deliveryMode.")
    }

    /**
     * Retrieves the original mail from the database or throws an exception if not found
     */
    private fun findOriginalMail(mailId: UUID): Mail {
        return mailRepository.findById(mailId).orElseThrow {
            ResourceNotFoundException("Original mail not found")
        }
    }
}
