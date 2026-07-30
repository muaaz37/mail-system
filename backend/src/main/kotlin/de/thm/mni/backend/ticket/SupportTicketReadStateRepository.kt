package de.thm.mni.backend.ticket

import org.springframework.data.repository.CrudRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface SupportTicketReadStateRepository : CrudRepository<SupportTicketReadState, UUID> {
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
