package de.thm.mni.backend.ticket.enums

/**
 * Lifecycle state of a support ticket.
 */
enum class SupportTicketStatus {
    OPEN,
    WAITING_FOR_SUPPORT,
    WAITING_FOR_CUSTOMER,
    RESOLVED
}
