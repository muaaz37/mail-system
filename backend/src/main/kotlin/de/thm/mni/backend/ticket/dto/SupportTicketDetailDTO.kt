package de.thm.mni.backend.ticket.dto

import de.thm.mni.backend.mail.dto.MailDTO

/**
 * Detail model used by the ticket conversation view.
 */
data class SupportTicketDetailDTO(
    val ticket: SupportTicketDTO,
    val mails: List<MailDTO>
)
