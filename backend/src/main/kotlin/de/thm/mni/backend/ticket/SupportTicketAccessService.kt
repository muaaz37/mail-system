package de.thm.mni.backend.ticket

import de.thm.mni.backend.error.ResourceNotFoundException
import de.thm.mni.backend.mail.enums.MailDeliveryMode
import de.thm.mni.backend.mail.enums.MailStatus
import de.thm.mni.backend.mailrecord.MailRecordService
import de.thm.mni.backend.user.User
import org.springframework.stereotype.Service

/**
 * Applies ticket visibility rules for shared external support cases and private internal inbox tickets.
 */
@Service
class SupportTicketAccessService(
    private val mailRecordService: MailRecordService
) {
    /**
     * External support tickets are shared. Internal tickets are visible only to their recipients.
     * @param ticket The ticket to check.
     * @param user The user to check.
     * @return True if the user can view the ticket.
     */
    fun canView(ticket: SupportTicket, user: User): Boolean {
        val hasExternalIncomingMail = ticket.mails.any { mail ->
            mail.deliveryMode == MailDeliveryMode.EXTERNAL &&
                mail.status == MailStatus.RECEIVED &&
                mail.sender == null
        }
        if (hasExternalIncomingMail) {
            return true
        }

        val userId = user.id ?: return false
        return ticket.mails.any { mail ->
            val mailId = mail.id ?: return@any false
            mailRecordService.getMailRecordByMailId(mailId).any { record ->
                record.user?.id == userId
            }
        }
    }

    /**
     * Ensures that the given user can view the given ticket. Throws a ResourceNotFoundException if not.
     * @param ticket The ticket to check.
     * @param user The user to check.
     * @throws ResourceNotFoundException If the user cannot view the ticket.
     */
    fun ensureVisible(ticket: SupportTicket, user: User) {
        if (!canView(ticket, user)) {
            throw ResourceNotFoundException("Support ticket not found")
        }
    }
}
