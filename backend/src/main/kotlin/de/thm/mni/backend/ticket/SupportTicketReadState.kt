package de.thm.mni.backend.ticket

import de.thm.mni.backend.user.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime
import java.util.UUID

/**
 * Stores the last time a specific support user opened a ticket.
 */
@Entity
@Table(
    name = "support_ticket_read_states",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_support_ticket_read_state_ticket_user",
            columnNames = ["ticket_id", "user_id"]
        )
    ]
)
class SupportTicketReadState {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    lateinit var ticket: SupportTicket

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    lateinit var user: User

    @Column(name = "read_at", nullable = false)
    var readAt: LocalDateTime = LocalDateTime.now()
}
