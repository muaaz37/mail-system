package de.thm.mni.backend.ticket

import de.thm.mni.backend.error.ResourceNotFoundException
import de.thm.mni.backend.ticket.enums.SupportTicketPriority
import de.thm.mni.backend.user.User
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Handles explicit user commands on support tickets.
 */
@Service
class SupportTicketCommandService(private val ticketRepository: SupportTicketRepository) {
    /**
     * Loads a support ticket or hides missing records behind the shared not-found error.
     *
     * @param ticketId Identifier of the support ticket.
     * @return Persisted support ticket.
     */
    fun ticketOrNotFound(ticketId: UUID): SupportTicket {
        return ticketRepository.findById(ticketId).orElse(null)
            ?: throw ResourceNotFoundException("Support ticket not found")
    }

    /**
     * Closes a support ticket after the team considers the conversation finished.
     *
     * @param ticketId Identifier of the support ticket.
     * @return Updated support ticket.
     */
    @Transactional
    fun resolveTicket(ticketId: UUID): SupportTicket {
        val ticket = ticketOrNotFound(ticketId)
        ticket.resolve()
        return ticketRepository.save(ticket)
    }

    /**
     * Reopens a resolved support ticket when work should continue.
     *
     * @param ticketId Identifier of the support ticket.
     * @return Updated support ticket.
     */
    @Transactional
    fun reopenTicket(ticketId: UUID): SupportTicket {
        val ticket = ticketOrNotFound(ticketId)
        ticket.reopen()
        return ticketRepository.save(ticket)
    }

    /**
     * Assigns a support ticket to the selected team user.
     *
     * @param ticketId Identifier of the support ticket.
     * @param user User who should own the next support step.
     * @return Updated support ticket.
     */
    @Transactional
    fun assignTo(ticketId: UUID, user: User): SupportTicket {
        val ticket = ticketOrNotFound(ticketId)
        ticket.assignTo(user)
        return ticketRepository.save(ticket)
    }

    /**
     * Clears the assignee of a support ticket.
     *
     * @param ticketId Identifier of the support ticket.
     * @return Updated support ticket.
     */
    @Transactional
    fun unassign(ticketId: UUID): SupportTicket {
        val ticket = ticketOrNotFound(ticketId)
        ticket.unassign()
        return ticketRepository.save(ticket)
    }

    /**
     * Updates the priority used for support queue triage.
     *
     * @param ticketId Identifier of the support ticket.
     * @param priority New priority selected by the user.
     * @return Updated support ticket.
     */
    @Transactional
    fun updatePriority(ticketId: UUID, priority: SupportTicketPriority): SupportTicket {
        val ticket = ticketOrNotFound(ticketId)
        ticket.changePriority(priority)
        return ticketRepository.save(ticket)
    }
}
