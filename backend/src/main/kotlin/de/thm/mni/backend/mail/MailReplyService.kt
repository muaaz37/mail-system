package de.thm.mni.backend.mail

import de.thm.mni.backend.error.ResourceNotFoundException
import de.thm.mni.backend.mail.dto.MailPayload
import de.thm.mni.backend.mail.dto.MailReplyTemplate
import de.thm.mni.backend.mail.enums.MailDeliveryMode
import de.thm.mni.backend.mail.external.SupportReplyService
import de.thm.mni.backend.mail.internal.InternalReplyService
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
    private val internalReplyService: InternalReplyService,
    private val supportReplyService: SupportReplyService
) {

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

        when (originalMail.deliveryMode) {
            MailDeliveryMode.INTERNAL ->
                internalReplyService.applyReplyContext(
                    replyMail = replyMail,
                    originalMail = originalMail
                )

            MailDeliveryMode.EXTERNAL ->
                supportReplyService.applyReplyContext(
                    mail = replyMail,
                    originalMail = originalMail
                )
        }
    }

    /**
     * Creates a reply template for the original mail's delivery channel.
     */
    @Transactional
    fun getReplyTemplate(
        originalMail: Mail,
        currentUser: User
    ): MailReplyTemplate {
        return when (originalMail.deliveryMode) {
            MailDeliveryMode.INTERNAL ->
                internalReplyService.getReplyTemplate(originalMail, currentUser)

            MailDeliveryMode.EXTERNAL ->
                supportReplyService.getReplyTemplate(originalMail)
        }
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

        when (originalMail.deliveryMode) {
            MailDeliveryMode.INTERNAL ->
                internalReplyService.enforceReplyContext(
                    originalMail = originalMail,
                    payload = payload,
                    currentUser = currentUser
                )

            MailDeliveryMode.EXTERNAL ->
                supportReplyService.enforceReplyRecipient(
                    payload = payload,
                    originalMail = originalMail
                )
        }
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
