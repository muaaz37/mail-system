package de.thm.mni.backend.mail

import de.thm.mni.backend.attachment.Attachment
import de.thm.mni.backend.mail.enums.MailStatus
import de.thm.mni.backend.user.User
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
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


@Entity
@Table(name = "mails")
class Mail {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id : UUID? = null

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    var sender: User? = null

    @Column
    var subject: String = ""

    @Column
    var content: String = ""

    @Column
    @Enumerated(EnumType.STRING)
    var status: MailStatus = MailStatus.DRAFT

    @OneToMany(mappedBy = "mail", cascade = [CascadeType.ALL], orphanRemoval = true)
    var attachments: MutableList<Attachment> = mutableListOf()


    @Column(name = "created_at", updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()

    @Column(name = "sent_at")
    var sentAt: LocalDateTime? = null

    constructor()

    constructor(sender: User, subject: String, content: String, attachments: MutableList<Attachment>) {
        this.sender = sender
        this.subject = subject
        this.content = content
        this.attachments = attachments
    }

    fun addAttachment(attachment: Attachment) {
        attachments.add(attachment)
        attachment.mail = this
    }

    fun removeAttachment(attachment: Attachment) {
        attachments.remove(attachment)
        attachment.mail = null
    }

    @PrePersist
    fun onCreate() {
        val now = LocalDateTime.now()
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    fun onUpdate() {
        if(status == MailStatus.SENT && sentAt == null) {
            sentAt = LocalDateTime.now()
        }else{
            updatedAt = LocalDateTime.now()
        }
    }

}