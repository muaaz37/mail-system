package de.thm.mni.backend.ticket

import de.thm.mni.backend.user.User
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class SupportTicketReadStateService(
    private val readStateRepository: SupportTicketReadStateRepository
) {
    fun hasUnreadActivity(ticket: SupportTicket, user: User, lastActivityAt: LocalDateTime): Boolean {
        val ticketId = ticket.id
        val userId = user.id
        if (ticketId == null || userId == null) {
            return false
        }

        val readState = readStateRepository.findReadState(ticketId, userId)

        return readState == null || readState.readAt.isBefore(lastActivityAt)
    }

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
