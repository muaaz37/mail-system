package de.thm.mni.backend.ticket

import de.thm.mni.backend.ticket.enums.SupportTicketStatus
import org.springframework.data.repository.CrudRepository
import java.util.UUID

/**
 * Database access for support tickets.
 */
interface SupportTicketRepository : CrudRepository<SupportTicket, UUID> {
    fun findByTicketNumber(ticketNumber: String): SupportTicket?
    fun existsByTicketNumber(ticketNumber: String): Boolean
    fun findAllByStatusIn(statuses: Collection<SupportTicketStatus>): List<SupportTicket>
}
