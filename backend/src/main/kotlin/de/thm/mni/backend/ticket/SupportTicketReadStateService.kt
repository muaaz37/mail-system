package de.thm.mni.backend.ticket

import de.thm.mni.backend.user.User
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * Tracks which ticket activity has already been seen by each support user.
 */
@Service
class SupportTicketReadStateService(
    private val readStateRepository: SupportTicketReadStateRepository
) {
    /**
     * Checks whether a ticket has activity newer than the user's stored read timestamp.
     *
     * @param ticket Support ticket shown in a queue or detail view.
     * @param user User whose read state should be evaluated.
     * @param lastActivityAt Latest activity timestamp derived from the ticket conversation.
     * @return True when the user has not seen the latest activity.
     */
    fun hasUnreadActivity(ticket: SupportTicket, user: User, lastActivityAt: LocalDateTime): Boolean {
        val ticketId = ticket.id
        val userId = user.id
        if (ticketId == null || userId == null) {
            return false
        }

        val readState = readStateRepository.findReadState(ticketId, userId)

        return readState == null || readState.readAt.isBefore(lastActivityAt)
    }

    /**
     * Stores that the user has seen the current ticket state.
     *
     * @param ticket Support ticket opened or updated by the user.
     * @param user User whose read timestamp should be updated.
     */
    @Transactional
    fun markRead(ticket: SupportTicket, user: User) {
        val ticketId = ticket.id ?: return
        val userId = user.id ?: return
        val readState = readStateRepository.findReadState(ticketId, userId)
            ?: SupportTicketReadState().apply {
                this.ticket = ticket
                this.user = user
            }

        readState.readAt = LocalDateTime.now()
        readStateRepository.save(readState)
    }
}
