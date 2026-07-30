package de.thm.mni.backend.ticket

import de.thm.mni.backend.ticket.dto.SupportTicketDTO
import de.thm.mni.backend.user.User
import de.thm.mni.backend.user.dto.toDTO
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * Converts ticket entities to API DTOs.
 */
@Component
class SupportTicketMapper(
    private val readStateService: SupportTicketReadStateService
) {
    fun toDTO(ticket: SupportTicket, user: User? = null): SupportTicketDTO {
        val lastActivityAt = lastActivityAt(ticket)

        return SupportTicketDTO(
            id = ticket.id,
            ticketNumber = ticket.ticketNumber,
            subject = ticket.subject,
            requesterEmail = ticket.requesterEmail,
            requesterName = ticket.requesterName,
            status = ticket.status,
            priority = ticket.priority,
            assignedTo = ticket.assignedTo?.toDTO(),
            mailCount = ticket.mails.size,
            hasUnreadActivity = user?.let { readStateService.hasUnreadActivity(ticket, it, lastActivityAt) } ?: false,
            lastActivityAt = lastActivityAt,
            createdAt = ticket.createdAt,
            updatedAt = ticket.updatedAt,
            closedAt = ticket.closedAt
        )
    }

    private fun lastActivityAt(ticket: SupportTicket): LocalDateTime {
        val latestMailActivity = ticket.mails.maxOfOrNull { mail -> mail.updatedAt }
        return listOfNotNull(latestMailActivity, ticket.updatedAt).maxOrNull() ?: ticket.updatedAt
    }
}
