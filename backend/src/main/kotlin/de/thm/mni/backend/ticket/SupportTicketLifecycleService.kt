package de.thm.mni.backend.ticket

import de.thm.mni.backend.error.InvalidMailRequestException
import de.thm.mni.backend.mail.Mail
import de.thm.mni.backend.mail.SupportTicketService
import de.thm.mni.backend.mail.enums.MailDeliveryMode
import de.thm.mni.backend.mail.enums.MailStatus
import de.thm.mni.backend.ticket.enums.SupportTicketStatus
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

/**
 * Owns support ticket lifecycle transitions and keeps mails attached to their ticket.
 */
@Service
class SupportTicketLifecycleService(
    private val ticketRepository: SupportTicketRepository,
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
     * Creates an inbox ticket for a successfully sent internal mail.
     */
    @Transactional
    fun attachInternalMail(mail: Mail): SupportTicket {
        if (mail.status != MailStatus.SENT || mail.deliveryMode != MailDeliveryMode.INTERNAL) {
            throw InvalidMailRequestException("Only sent internal mails can open internal tickets.")
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
        val ticketNumber = mail.ticketNumber
            ?: supportTicketService.extractTicketNumber(mail.subject)
            ?: supportTicketService.generateUniqueTicketNumber()

        val existingTicket = ticketRepository.findByTicketNumber(ticketNumber)
        if (existingTicket != null) {
            return existingTicket
        }

        return SupportTicket().apply {
            this.ticketNumber = ticketNumber
            status = SupportTicketStatus.WAITING_FOR_SUPPORT
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
        // For internal mails, use the sender's name and email if available
        if (mail.deliveryMode == MailDeliveryMode.INTERNAL) {
            val sender = mail.sender
            if (ticket.requesterEmail == null) {
                ticket.requesterEmail = sender?.email
            }
            if (ticket.requesterName == null) {
                ticket.requesterName = sender?.let { "${it.firstName} ${it.lastName}".trim() }
            }
        }
    }
}
