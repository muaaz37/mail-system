package de.thm.mni.backend.ticket.dto

import de.thm.mni.backend.ticket.enums.SupportTicketPriority

/**
 * Request body for changing ticket priority.
 */
data class UpdateTicketPriorityRequest(
    val priority: SupportTicketPriority
)
