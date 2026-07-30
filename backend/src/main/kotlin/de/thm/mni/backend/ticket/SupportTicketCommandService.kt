package de.thm.mni.backend.ticket

import de.thm.mni.backend.error.ResourceNotFoundException
import de.thm.mni.backend.ticket.enums.SupportTicketPriority
import de.thm.mni.backend.ticket.enums.SupportTicketStatus
import de.thm.mni.backend.user.User
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

/**
 * Handles explicit user commands on support tickets.
 */
@Service
class SupportTicketCommandService(private val ticketRepository: SupportTicketRepository) {
    fun ticketOrNotFound(ticketId: UUID): SupportTicket {
        return ticketRepository.findById(ticketId).orElse(null)
            ?: throw ResourceNotFoundException("Support ticket not found")
    }

    @Transactional
    fun resolveTicket(ticketId: UUID): SupportTicket {
        val ticket = ticketOrNotFound(ticketId)
        ticket.status = SupportTicketStatus.RESOLVED
        ticket.closedAt = LocalDateTime.now()
        return ticketRepository.save(ticket)
    }

    @Transactional
    fun reopenTicket(ticketId: UUID): SupportTicket {
        val ticket = ticketOrNotFound(ticketId)
        ticket.status = SupportTicketStatus.WAITING_FOR_SUPPORT
        ticket.closedAt = null
        return ticketRepository.save(ticket)
    }

    @Transactional
    fun assignTo(ticketId: UUID, user: User): SupportTicket {
        val ticket = ticketOrNotFound(ticketId)
        ticket.assignedTo = user
        return ticketRepository.save(ticket)
    }

    @Transactional
    fun unassign(ticketId: UUID): SupportTicket {
        val ticket = ticketOrNotFound(ticketId)
        ticket.assignedTo = null
        return ticketRepository.save(ticket)
    }

    @Transactional
    fun updatePriority(ticketId: UUID, priority: SupportTicketPriority): SupportTicket {
        val ticket = ticketOrNotFound(ticketId)
        ticket.priority = priority
        return ticketRepository.save(ticket)
    }
}
