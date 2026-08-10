package de.thm.mni.backend.ticket

import de.thm.mni.backend.mail.Mail
import de.thm.mni.backend.ticket.enums.SupportTicketPriority
import de.thm.mni.backend.ticket.enums.SupportTicketStatus
import de.thm.mni.backend.user.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

/**
 * Persistent support case that groups all external incoming and support reply mails for one ticket number.
 */
@Entity
@Table(name = "support_tickets")
class SupportTicket {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null

    @Column(name = "ticket_number", unique = true, nullable = false)
    var ticketNumber: String = ""

    @Column(length = SUBJECT_COLUMN_LENGTH)
    var subject: String = ""

    @Column(name = "requester_email")
    var requesterEmail: String? = null

    @Column(name = "requester_name")
    var requesterName: String? = null

    @Column
    @Enumerated(EnumType.STRING)
    var status: SupportTicketStatus = SupportTicketStatus.WAITING_FOR_SUPPORT
        private set

    @Column
    @Enumerated(EnumType.STRING)
    var priority: SupportTicketPriority = SupportTicketPriority.NORMAL
        private set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    var assignedTo: User? = null
        private set

    @OneToMany(mappedBy = "ticket")
    var mails: MutableList<Mail> = mutableListOf()

    @Column(name = "created_at", updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()

    @Column(name = "closed_at")
    var closedAt: LocalDateTime? = null
        private set

    fun assignTo(user: User) {
        assignedTo = user
    }

    fun unassign() {
        assignedTo = null
    }

    fun resolve() {
        status = SupportTicketStatus.RESOLVED
        closedAt = LocalDateTime.now()
    }

    fun reopen() {
        status = SupportTicketStatus.WAITING_FOR_SUPPORT
        closedAt = null
    }

    fun markWaitingForSupport() {
        status = SupportTicketStatus.WAITING_FOR_SUPPORT
        closedAt = null
    }

    fun markWaitingForCustomer() {
        status = SupportTicketStatus.WAITING_FOR_CUSTOMER
        closedAt = null
    }

    fun changePriority(priority: SupportTicketPriority) {
        this.priority = priority
    }

    @PrePersist
    fun onCreate() {
        val now = LocalDateTime.now()
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    fun onUpdate() {
        updatedAt = LocalDateTime.now()
    }

    private companion object {
        const val SUBJECT_COLUMN_LENGTH = 500
    }
}
