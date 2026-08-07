package de.thm.mni.backend.ticket

import de.thm.mni.backend.error.InvalidMailRequestException
import de.thm.mni.backend.mail.Mail
import de.thm.mni.backend.mail.MailRepository
import de.thm.mni.backend.mail.enums.MailDeliveryMode
import de.thm.mni.backend.mail.enums.MailStatus
import de.thm.mni.backend.mail.external.toMessageIdList
import de.thm.mni.backend.ticket.enums.SupportTicketStatus
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

/**
 * Owns support ticket lifecycle transitions and keeps mails attached to their ticket.
 */
@Service
class SupportTicketLifecycleService(
    private val ticketRepository: SupportTicketRepository,
    private val mailRepository: MailRepository,
    private val supportTicketService: SupportTicketService,
    private val readStateService: SupportTicketReadStateService
) {
    /**
     * Lists support tickets filtered by the requested workflow view.
     * Supported values are `open`, `waiting`, `resolved`, and `all`; unknown values use the `open` view.
     */
    fun listTickets(view: String?): List<SupportTicket> {
        val statuses = when (view?.lowercase()) {
            "waiting" -> listOf(SupportTicketStatus.WAITING_FOR_CUSTOMER)
            "resolved" -> listOf(SupportTicketStatus.RESOLVED)
            "all" -> return ticketRepository.findAll().toList().sortedByDescending { ticket -> ticket.updatedAt }
            else -> listOf(SupportTicketStatus.OPEN, SupportTicketStatus.WAITING_FOR_SUPPORT)
        }

        return ticketRepository.findAllByStatusIn(statuses).sortedByDescending { ticket -> ticket.updatedAt }
    }

    /**
     * Ensures that an incoming external mail belongs to a ticket and reopens resolved tickets.
     */
    @Transactional
    fun attachIncomingMail(mail: Mail): SupportTicket {
        if (mail.status != MailStatus.RECEIVED || mail.deliveryMode != MailDeliveryMode.EXTERNAL) {
            throw InvalidMailRequestException("Only imported external mails can open support tickets.")
        }

        val ticket = findOrCreateTicket(mail)
        ticket.status = SupportTicketStatus.WAITING_FOR_SUPPORT
        ticket.closedAt = null
        enrichTicketFromMail(ticket, mail)
        mail.ticketNumber = ticket.ticketNumber
        mail.ticket = ticket
        return ticketRepository.save(ticket)
    }

    /**
     * Assigns a draft or sent support reply to the existing original ticket.
     * @param mail The reply mail.
     * @param originalMail The original mail.
     * @return The existing ticket for the original mail.
     */
    @Transactional
    fun attachReplyMail(mail: Mail, originalMail: Mail): SupportTicket {
        val ticket = ensureTicketForMail(originalMail)
        mail.ticketNumber = ticket.ticketNumber
        mail.ticket = ticket
        return ticket
    }

    /**
     * Ensures that a visible incoming mail has a ticket before building a reply.
     */
    @Transactional
    fun ensureTicketForMail(mail: Mail): SupportTicket {
        val ticket = findOrCreateTicket(mail)
        mail.ticketNumber = ticket.ticketNumber
        mail.ticket = ticket
        enrichTicketFromMail(ticket, mail)
        return ticketRepository.save(ticket)
    }

    /**
     * Marks the ticket as waiting for customer response after sending an external reply.
     */
    @Transactional
    fun markWaitingForCustomer(mail: Mail) {
        val ticket = mail.ticket ?: return
        ticket.status = SupportTicketStatus.WAITING_FOR_CUSTOMER
        ticket.closedAt = null
        val savedTicket = ticketRepository.save(ticket)
        mail.sender?.let { sender -> readStateService.markRead(savedTicket, sender) }
    }

    private fun findOrCreateTicket(mail: Mail): SupportTicket {
        return mail.ticket
            ?: findTicketByThreadReferences(mail)
            ?: findTicketByTrustedSubject(mail)
            ?: SupportTicket().apply {
                ticketNumber = supportTicketService.generateUniqueTicketNumber()
                status = SupportTicketStatus.WAITING_FOR_SUPPORT
            }
    }

    /**
     * Resolves customer replies through RFC mail-thread headers before trusting the editable subject.
     */
    private fun findTicketByThreadReferences(mail: Mail): SupportTicket? {
        val referencedMessageIds = (
            mail.externalInReplyTo.toMessageIdList() +
                mail.externalReferences.toMessageIdList()
            ).distinct()

        return referencedMessageIds.asSequence()
            .mapNotNull { messageId -> mailRepository.findByExternalMessageId(messageId)?.ticket }
            .firstOrNull()
    }

    /**
     * Uses a subject ticket only when it points to an existing ticket owned by the same requester.
     */
    private fun findTicketByTrustedSubject(mail: Mail): SupportTicket? {
        val ticket = supportTicketService.extractTicketNumber(mail.subject)
            ?.let { ticketNumber -> ticketRepository.findByTicketNumber(ticketNumber) }
        val requesterEmail = ticket?.requesterEmail
        val senderEmail = mail.externalSenderEmail
        val requesterMatchesSender = requesterEmail != null &&
            senderEmail != null &&
            requesterEmail.equals(senderEmail, ignoreCase = true)

        return if (requesterMatchesSender) {
            ticket
        } else {
            null
        }
    }

    /**
     * Enriches the ticket with subject, requester email, and requester name from the mail if they are not already set.
     */
    private fun enrichTicketFromMail(ticket: SupportTicket, mail: Mail) {
        if (ticket.subject.isBlank()) {
            ticket.subject = supportTicketService.removeTicketPrefix(mail.subject)
        }
        if (ticket.requesterEmail == null) {
            ticket.requesterEmail = mail.externalSenderEmail
        }
        if (ticket.requesterName == null) {
            ticket.requesterName = mail.externalSenderName
        }
    }
}
