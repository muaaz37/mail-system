package de.thm.mni.backend.mail_record

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.io.Serializable
import java.util.UUID


@Embeddable
class MailRecordId: Serializable {

    @Column(name = "mail_id")
    var mailId: UUID? = null

    @Column(name = "user_id")
    var userId:  UUID? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MailRecordId) return false

        if (mailId != other.mailId) return false
        if (userId != other.userId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = mailId?.hashCode() ?: 0
        result = 31 * result + (userId?.hashCode() ?: 0)
        return result
    }

    constructor()

    constructor(mailId: UUID, userId: UUID) {
        this.mailId = mailId
        this.userId = userId
    }

}