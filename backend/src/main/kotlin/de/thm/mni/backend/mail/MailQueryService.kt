package de.thm.mni.backend.mail

import de.thm.mni.backend.mail.enums.MailStatus
import de.thm.mni.backend.mailrecord.MailRecordService
import de.thm.mni.backend.user.User
import org.springframework.stereotype.Service
import java.util.UUID

/** Provides read-only mailbox queries without coordinating mail commands. */
@Service
class MailQueryService(
    private val mailRepository: MailRepository,
    private val mailRecordService: MailRecordService
) {
    /**
     * Returns a mail by its ID or null if it does not exist.
     */
    fun getMailById(id: UUID): Mail? = mailRepository.findById(id).orElse(null)

    /**
     * Returns all mails created by the user that are still in draft status.
     */
    fun getAllCreatedUserMails(user: User): List<Mail> {
        return mailRepository.findAllBySender(user)
            .filter { mail -> mail.status == MailStatus.DRAFT }
    }

    /**
     * Returns all mails created by the user that have been sent.
     */
    fun getAllSentUserMails(user: User): List<Mail> {
        return mailRepository.findAllBySender(user)
            .filter { mail -> mail.status == MailStatus.SENT }
            .sortedByDescending { mail -> mail.sentAt ?: mail.createdAt }
    }

    /**
     * Returns all incoming mails for the specified user.
     */
    fun getIncomingMailsForUser(userId: UUID): List<Mail> {
        val internalMails = mailRecordService.getAllIncomingMailsForUser(userId)
        val supportMails = mailRepository.findAllByStatus(MailStatus.RECEIVED)
            .filter { mail -> mail.sender == null }
        return (internalMails + supportMails).distinctBy { mail -> mail.id }
    }
}
