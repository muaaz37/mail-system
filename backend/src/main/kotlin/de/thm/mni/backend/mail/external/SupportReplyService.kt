package de.thm.mni.backend.mail.external

import de.thm.mni.backend.error.InvalidMailRequestException
import de.thm.mni.backend.error.ResourceNotFoundException
import de.thm.mni.backend.mail.Mail
import de.thm.mni.backend.mail.MailRepository
import de.thm.mni.backend.ticket.SupportTicketService
import de.thm.mni.backend.mail.dto.MailPayload
import de.thm.mni.backend.mail.dto.ExternalMailReplyTemplate
import de.thm.mni.backend.mail.enums.MailDeliveryMode
import de.thm.mni.backend.mail.enums.MailStatus
import de.thm.mni.backend.mail.toRecipientList
import de.thm.mni.backend.mail.toRecipientString
import de.thm.mni.backend.ticket.SupportTicketLifecycleService
import jakarta.mail.internet.AddressException
import jakarta.mail.internet.InternetAddress
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Builds and applies reply metadata for incoming support mails.
 */
@Service
class SupportReplyService(
    private val mailRepository: MailRepository,
    private val supportTicketService: SupportTicketService,
    private val supportTicketLifecycleService: SupportTicketLifecycleService
) {
    /**
     * Creates a reply template with ticket subject and the best external recipient address.
     */
    @Transactional
    fun getReplyTemplate(originalMail: Mail): ExternalMailReplyTemplate {
        ensureCanReplyToSupportMail(originalMail)
        val ticket = supportTicketLifecycleService.ensureTicketForMail(originalMail)
        val ticketNumber = ticket.ticketNumber
        val recipient = replyRecipientFor(originalMail)

        return ExternalMailReplyTemplate(
            replyToMailId = requireNotNull(originalMail.id),
            ticketNumber = ticketNumber,
            subject = supportTicketService.buildReplySubject(
                originalMail.subject,
                ticketNumber
            ),
            recipients = listOf(recipient)
        )
    }

    /**
     * Replaces client-supplied primary recipients with the original support sender.
     */
    @Transactional
    fun enforceReplyRecipient(payload: MailPayload, originalMail: Mail)  {
        if (payload.deliveryMode != MailDeliveryMode.EXTERNAL) {
            throw InvalidMailRequestException(
                "Support replies must be external mails."
            )
        }

        ensureCanReplyToSupportMail(originalMail)

        payload.externalTo.clear()
        payload.externalTo.add(
            replyRecipientFor(originalMail)
        )
    }

    /**
     * Keeps the locked primary recipient when a stored support reply draft is edited later.
     */
    fun enforceStoredReplyRecipient(existingMail: Mail, payload: MailPayload) {
        if (!existingMail.hasSupportTicketContext() || payload.replyToMailId != null) {
            return
        }
        if (payload.deliveryMode != MailDeliveryMode.EXTERNAL) {
            throw InvalidMailRequestException("Support replies must be external mails.")
        }

        payload.externalTo.clear()
        payload.externalTo.addAll(existingMail.externalTo.toRecipientList())
    }

    /**
     * Copies the original support ticket context to a new outgoing reply.
     *
     * @param mail The outgoing reply mail.
     * @param originalMail The original support mail.
     */
    @Transactional
    fun applyReplyContext(mail: Mail, originalMail: Mail) {
        if (mail.deliveryMode != MailDeliveryMode.EXTERNAL) {
            throw InvalidMailRequestException(
                "Support replies must use external delivery."
            )
        }

        ensureCanReplyToSupportMail(originalMail)

        mail.inReplyToMail = originalMail
        val ticket = supportTicketLifecycleService.attachReplyMail(
            mail,
            originalMail
        )

        mail.externalTo = listOf(
            replyRecipientFor(originalMail)
        ).toRecipientString()

        mail.externalInReplyTo = originalMail.externalMessageId
        mail.externalReferences = referencesForReply(originalMail)
        mail.subject = supportTicketService.prependTicketIfMissing(
            mail.subject,
            ticket.ticketNumber
        )
    }

    /**
     * Restores the ticket prefix before an external support reply is sent.
     */
    fun enforceTicketSubject(mail: Mail) {
        val ticketNumber = mail.ticket?.ticketNumber ?: mail.ticketNumber ?: return
        mail.ticketNumber = ticketNumber
        mail.subject = supportTicketService.prependTicketIfMissing(mail.subject, ticketNumber)
    }

    private fun referencesForReply(originalMail: Mail): String {
        return (
            originalMail.externalReferences.toMessageIdList() +
                originalMail.externalMessageId.toMessageIdList()
            ).toMessageIdHeaderValue()
    }

    private fun replyRecipientFor(originalMail: Mail): String {
        return originalMail.externalReplyTo.toRecipientList()
            .firstNotNullOfOrNull { recipient -> recipient.emailAddress() }
            ?: originalMail.externalSenderEmail.emailAddress()
            ?: throw InvalidMailRequestException("Incoming support mail has no reply recipient.")
    }

    private fun getMailById(mailId: UUID): Mail {
        return mailRepository.findById(mailId).orElse(null) ?: throw ResourceNotFoundException("Mail not found")
    }

    /**
     * Ensures that only imported support mails with a reply recipient can be answered.
     */
    private fun ensureCanReplyToSupportMail(mail: Mail) {
        if (mail.status != MailStatus.RECEIVED || mail.deliveryMode != MailDeliveryMode.EXTERNAL) {
            throw InvalidMailRequestException("Only incoming support mails can be answered with a ticket.")
        }
        val hasReplyRecipient = mail.externalReplyTo.toRecipientList()
            .any { recipient -> recipient.emailAddress() != null } ||
            mail.externalSenderEmail.emailAddress() != null
        if (!hasReplyRecipient) {
            throw InvalidMailRequestException("Incoming support mail has no reply recipient.")
        }
    }
}

private fun Mail.hasSupportTicketContext(): Boolean {
    return deliveryMode == MailDeliveryMode.EXTERNAL && (ticket != null || ticketNumber != null)
}

/**
 * Extracts a plain email address from a stored address string.
 */
private fun String?.emailAddress(): String? {
    val value = this?.trim()?.takeIf { text -> text.isNotBlank() } ?: return null
    return try {
        InternetAddress.parse(value, false)
            .firstOrNull()
            ?.address
            ?.trim()
            ?.takeIf { address -> address.isNotBlank() }
    } catch (_: AddressException) {
        null
    }
}
