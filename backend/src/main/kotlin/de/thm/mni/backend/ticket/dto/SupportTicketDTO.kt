package de.thm.mni.backend.ticket.dto

import de.thm.mni.backend.ticket.enums.SupportTicketPriority
import de.thm.mni.backend.ticket.enums.SupportTicketStatus
import de.thm.mni.backend.user.dto.UserDTO
import java.time.LocalDateTime
import java.util.UUID

/**
 * Summary model used by ticket lists.
 */
data class SupportTicketDTO(
    val id: UUID?,
    val ticketNumber: String,
    val subject: String,
    val requesterEmail: String?,
    val requesterName: String?,
    val status: SupportTicketStatus,
    val priority: SupportTicketPriority,
    val assignedTo: UserDTO?,
    val mailCount: Int,
    val hasUnreadActivity: Boolean,
    val lastActivityAt: LocalDateTime,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val closedAt: LocalDateTime?
)
