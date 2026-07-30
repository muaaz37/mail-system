package de.thm.mni.backend.mailrecord

import de.thm.mni.backend.error.ResourceNotFoundException
import de.thm.mni.backend.mail.Mail
import de.thm.mni.backend.mail.enums.MailStatus
import de.thm.mni.backend.mail.enums.MailType
import de.thm.mni.backend.mailrecord.dto.CreateMailRecord
import de.thm.mni.backend.user.User
import de.thm.mni.backend.user.UserService
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Manages recipient records for internal mails.
 */
@Service
class MailRecordService(
    private val repository: MailRecordRepository,
    private val userService: UserService
) {
    /**
     * Creates one recipient record for a mail and receiver.
     */
    @Transactional
    fun createMailRecord(mailRecord: CreateMailRecord): MailRecord {
        val mailRecordEntity = MailRecord(
            mail = mailRecord.mail,
            user = mailRecord.receiver,
            type = mailRecord.mailType
        )
        return repository.save(mailRecordEntity)
    }

    /**
     * Deletes one recipient record by its composite key.
     */
    @Transactional
    fun deleteMailRecord(id: MailRecordId) {
        repository.deleteById(id)
    }

    /**
     * Returns all recipient records linked to a mail.
     */
    fun getMailRecordByMailId(mailId: UUID): List<MailRecord> {
        return repository.findMailRecordByMailId(mailId)
    }

    /**
     * Returns sent internal mails where the user is a recipient.
     */
    fun getAllIncomingMailsForUser(userId: UUID): List<Mail> {
        return repository.findAllByUserId(userId)
            .filter { record -> record.type !== MailType.REPLY_TO }
            .map { record -> record.mail!! }
            .filter { mail -> mail.status == MailStatus.SENT }
    }

    /**
     * Creates all recipient records for To, Cc, Bcc and Reply-To lists.
     */
    fun createMailRecordsFromIds(
        mail: Mail,
        toIds: List<UUID>,
        ccIds: List<UUID>,
        bccIds: List<UUID>,
        replyToIds: List<UUID>
    ) {
        createMailRecords(mail, toIds, MailType.TO)
        createMailRecords(mail, ccIds, MailType.CC)
        createMailRecords(mail, bccIds, MailType.BCC)
        createMailRecords(mail, replyToIds, MailType.REPLY_TO)
    }

    /**
     * Resolves receiver IDs and stores records for one recipient type.
     */
    private fun createMailRecords(mail: Mail, receiverIds: List<UUID>, mailType: MailType) {
        receiverIds.forEach { id ->
            createMailRecord(
                CreateMailRecord(
                    mail = mail,
                    receiver = receiverById(id),
                    mailType = mailType
                )
            )
        }
    }

    private fun receiverById(id: UUID): User {
        return userService.getUserById(id) ?: throw ResourceNotFoundException("Receiver not found")
    }
}
