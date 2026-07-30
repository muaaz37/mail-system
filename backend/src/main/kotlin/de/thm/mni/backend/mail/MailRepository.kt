package de.thm.mni.backend.mail

import de.thm.mni.backend.mail.enums.MailStatus
import de.thm.mni.backend.user.User
import org.springframework.data.repository.CrudRepository
import java.util.UUID

/**
 * Provides database access for mail entities and support ticket lookup checks.
 */
interface MailRepository: CrudRepository<Mail, UUID> {
    fun findAllBySender(sender: User): MutableList<Mail>
    fun findAllByStatus(status: MailStatus): MutableList<Mail>
    fun existsByExternalMessageId(externalMessageId: String): Boolean
    fun existsByTicketNumber(ticketNumber: String): Boolean
}
