package de.thm.mni.backend.mail

import de.thm.mni.backend.attachment.Attachment
import de.thm.mni.backend.mail.enums.MailDeliveryMode
import de.thm.mni.backend.mail.enums.MailStatus
import de.thm.mni.backend.ticket.SupportTicket
import de.thm.mni.backend.user.User
import jakarta.persistence.CascadeType
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
 * Persistent mail entity for drafts, sent mails, imported support mails and attachments.
 */
@Entity
@Table(name = "mails")
class Mail {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = true)
    var sender: User? = null

    /**
     * Original mail answered by this mail.
     *
     * The relation is stored for internal and external replies so that reply
     * context survives draft creation, editing and application restarts.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "in_reply_to_mail_id")
    var inReplyToMail: Mail? = null

    @Column(length = SUBJECT_COLUMN_LENGTH)
    var subject: String = ""

    @Column(columnDefinition = "TEXT")
    var content: String = ""

    @Column
    @Enumerated(EnumType.STRING)
    var status: MailStatus = MailStatus.DRAFT

    @Column(name = "delivery_mode")
    @Enumerated(EnumType.STRING)
    var deliveryMode: MailDeliveryMode = MailDeliveryMode.INTERNAL

    @OneToMany(mappedBy = "mail", cascade = [CascadeType.ALL], orphanRemoval = true)
    var attachments: MutableList<Attachment> = mutableListOf()

    @Column(name = "created_at", updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()

    @Column(name = "sent_at")
    var sentAt: LocalDateTime? = null

    @Column(name = "external_sender_email")
    var externalSenderEmail: String? = null

    @Column(name = "external_sender_name")
    var externalSenderName: String? = null

    @Column(name = "external_message_id", unique = true)
    var externalMessageId: String? = null

    @Column(name = "external_in_reply_to", length = THREAD_HEADER_COLUMN_LENGTH)
    var externalInReplyTo: String? = null

    @Column(name = "external_references", columnDefinition = "TEXT")
    var externalReferences: String? = null

    @Column(name = "external_sent_at")
    var externalSentAt: LocalDateTime? = null

    @Column(name = "ticket_number")
    var ticketNumber: String? = null

    @ManyToOne
    @JoinColumn(name = "ticket_id")
    var ticket: SupportTicket? = null

    @Column(name = "external_to", length = RECIPIENT_COLUMN_LENGTH)
    var externalTo: String = ""

    @Column(name = "external_cc", length = RECIPIENT_COLUMN_LENGTH)
    var externalCc: String = ""

    @Column(name = "external_bcc", length = RECIPIENT_COLUMN_LENGTH)
    var externalBcc: String = ""

    @Column(name = "external_reply_to", length = RECIPIENT_COLUMN_LENGTH)
    var externalReplyTo: String = ""

    constructor()

    constructor(sender: User, subject: String, content: String, attachments: MutableList<Attachment>) {
        this.sender = sender
        this.subject = subject
        this.content = content
        this.attachments = attachments
    }

    /**
     * Adds an attachment and keeps the JPA relation consistent.
     */
    fun addAttachment(attachment: Attachment) {
        attachments.add(attachment)
        attachment.mail = this
    }

    /**
     * Removes an attachment and clears its relation to this mail.
     */
    fun removeAttachment(attachment: Attachment) {
        attachments.remove(attachment)
        attachment.mail = null
    }

    /**
     * Initializes timestamps before the mail is stored for the first time.
     */
    @PrePersist
    fun onCreate() {
        val now = LocalDateTime.now()
        createdAt = now
        updatedAt = now
    }

    /**
     * Updates timestamps and sets the sent time when a mail is sent.
     */
    @PreUpdate
    fun onUpdate() {
        if (status == MailStatus.SENT && sentAt == null) {
            sentAt = LocalDateTime.now()
        } else {
            updatedAt = LocalDateTime.now()
        }
    }

    private companion object {
        const val SUBJECT_COLUMN_LENGTH = 500
        const val RECIPIENT_COLUMN_LENGTH = 1000
        const val THREAD_HEADER_COLUMN_LENGTH = 1000
    }
}
