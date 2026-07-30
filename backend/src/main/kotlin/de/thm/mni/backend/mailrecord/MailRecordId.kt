package de.thm.mni.backend.mailrecord

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.io.Serializable
import java.util.UUID

/**
 * Composite key for the mail-to-user recipient relation.
 */
@Embeddable
class MailRecordId : Serializable {
    @Column(name = "mail_id")
    var mailId: UUID? = null

    @Column(name = "user_id")
    var userId: UUID? = null

    constructor()

    constructor(mailId: UUID, userId: UUID) {
        this.mailId = mailId
        this.userId = userId
    }

    /**
     * Compares both mail and user identifiers for composite-key equality.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MailRecordId) return false
        if (mailId != other.mailId) return false
        if (userId != other.userId) return false
        return true
    }

    /**
     * Builds a stable hash from both composite-key fields.
     */
    override fun hashCode(): Int {
        var result = mailId?.hashCode() ?: 0
        result = HASH_MULTIPLIER * result + (userId?.hashCode() ?: 0)
        return result
    }

    private companion object {
        private const val serialVersionUID = 1L
        const val HASH_MULTIPLIER = 31
    }
}
