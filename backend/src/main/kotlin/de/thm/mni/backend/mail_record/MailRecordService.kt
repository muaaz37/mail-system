package de.thm.mni.backend.mail_record

import de.thm.mni.backend.mail.Mail
import de.thm.mni.backend.mail.enums.MailStatus
import de.thm.mni.backend.mail.enums.MailType
import de.thm.mni.backend.mail_record.dto.CreateMailRecord
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.util.UUID


@Service
class MailRecordService(private val repository: MailRecordRepository) {

    @Transactional
    fun createMailRecord(mailRecord: CreateMailRecord): MailRecord {
        val mailRecordEntity = MailRecord(
            mail = mailRecord.mail,
            user = mailRecord.receiver,
            type = mailRecord.mailType
        )
        return repository.save(mailRecordEntity)
    }

    @Transactional
    fun deleteMailRecord(id: MailRecordId) {
        repository.deleteById(id)
    }

    fun getMailRecordByMailId(mailId: UUID): List<MailRecord> {
        return repository.findMailRecordByMailId(mailId)
    }

    fun getAllIncomingMailsForUser(userId: UUID): List<Mail> {
        return repository.findAllByUserId(userId)
            .filter { it -> it.type !== MailType.REPLY_TO }
            .map { it -> it.mail!! }
            .filter { mail -> mail.status == MailStatus.SENT }
    }

}