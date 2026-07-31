package de.thm.mni.backend.ticket.dto

import de.thm.mni.backend.ticket.enums.SupportTicketPriority
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Request body for changing ticket priority.
 */
@Schema(description = "Request payload for changing a support ticket's priority.")
data class UpdateTicketPriorityRequest(
    @field:Schema(description = "New ticket priority.", example = "HIGH")
    val priority: SupportTicketPriority
)
