package de.thm.mni.backend.mail

import de.thm.mni.backend.error.InvalidMailRequestException
import de.thm.mni.backend.error.ResourceNotFoundException
import de.thm.mni.backend.mail.dto.MailReplyTemplate
import de.thm.mni.backend.mail.enums.MailDeliveryMode
import de.thm.mni.backend.mail.enums.MailStatus
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
    fun getReplyTemplate(originalMail: Mail): MailReplyTemplate {
        ensureCanReplyToSupportMail(originalMail)
        val ticket = supportTicketLifecycleService.ensureTicketForMail(originalMail)
        val ticketNumber = ticket.ticketNumber
        val recipient = originalMail.externalReplyTo.toRecipientList()
            .firstNotNullOfOrNull { recipient -> recipient.emailAddress() }
            ?: originalMail.externalSenderEmail.emailAddress()
            ?: throw InvalidMailRequestException("Incoming support mail has no reply recipient.")

        return MailReplyTemplate(
            replyToMailId = originalMail.id!!,
            ticketNumber = ticketNumber,
            subject = supportTicketService.buildReplySubject(originalMail.subject, ticketNumber),
            externalTo = listOf(recipient)
        )
    }

    /**
     * Copies the original support ticket context to a new outgoing reply.
     */
    @Transactional
    fun applyReplyContext(mail: Mail, replyToMailId: UUID?) {
        if (replyToMailId == null) {
            return
        }
        if (mail.deliveryMode != MailDeliveryMode.EXTERNAL) {
            throw InvalidMailRequestException("Support replies must be external mails.")
        }

        val originalMail = getMailById(replyToMailId)
        ensureCanReplyToSupportMail(originalMail)
        val ticket = supportTicketLifecycleService.attachReplyMail(mail, originalMail)
        mail.subject = supportTicketService.prependTicketIfMissing(mail.subject, ticket.ticketNumber)
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
