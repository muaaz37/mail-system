package de.thm.mni.backend.ticket

import org.springframework.data.repository.CrudRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

/**
 * Persists per-user ticket read markers used to highlight unread support activity.
 */
interface SupportTicketReadStateRepository : CrudRepository<SupportTicketReadState, UUID> {
    /**
     * Returns the read marker for a specific ticket/user pair.
     */
    @Query(
        """
        select state
        from SupportTicketReadState state
        where state.ticket.id = :ticketId and state.user.id = :userId
        """
    )
    fun findReadState(
        @Param("ticketId") ticketId: UUID,
        @Param("userId") userId: UUID
    ): SupportTicketReadState?
}
