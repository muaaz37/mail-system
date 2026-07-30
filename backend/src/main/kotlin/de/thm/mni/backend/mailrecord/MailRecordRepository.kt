package de.thm.mni.backend.mailrecord

import org.springframework.data.repository.CrudRepository
import java.util.UUID

/**
 * Provides database access for internal mail recipient records.
 */
interface MailRecordRepository : CrudRepository<MailRecord, MailRecordId> {
    fun findMailRecordByMailId(mailId: UUID): MutableList<MailRecord>
    fun findAllByUserId(userId: UUID): MutableList<MailRecord>
}
