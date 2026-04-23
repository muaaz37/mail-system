package de.thm.mni.backend.mail_record

import de.thm.mni.backend.mail.Mail
import de.thm.mni.backend.mail.enums.MailType
import de.thm.mni.backend.user.User
import jakarta.persistence.Column
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table


@Entity
@Table(name = "mail_records")
class MailRecord {

    @EmbeddedId
    var id: MailRecordId? = null

    @ManyToOne
    @JoinColumn(name = "mail_id", insertable = false, updatable = false)
    var mail: Mail? = null

    @ManyToOne
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    var user: User? = null

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    var type: MailType? = null

    constructor()

    constructor(mail: Mail, user: User, type: MailType) {
        this.mail = mail
        this.user = user
        this.type = type
        this.id = MailRecordId(mail.id!!, user.id!!)
    }

}