package de.thm.mni.backend.mail

import de.thm.mni.backend.attachment.Attachment
import de.thm.mni.backend.attachment.dto.AttachmentDTO
import de.thm.mni.backend.error.ResourceNotFoundException
import de.thm.mni.backend.mail.dto.MailCreate
import de.thm.mni.backend.mail.dto.MailUpdate
import de.thm.mni.backend.mail.enums.MailStatus
import de.thm.mni.backend.mail.enums.MailType
import de.thm.mni.backend.mail_record.MailRecordService
import de.thm.mni.backend.mail_record.dto.CreateMailRecord
import de.thm.mni.backend.smtp.SMTPService
import de.thm.mni.backend.storage.FileStorageService
import de.thm.mni.backend.user.User
import de.thm.mni.backend.user.UserService
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.util.UUID


@Service
class MailService(
    private val mailRepository: MailRepository,
    private val userService: UserService,
    private val smtpService: SMTPService,
    private val fileStorageService: FileStorageService,
    private val mailRecordService: MailRecordService
){
    fun getMailById(id: UUID): Mail? {
        return mailRepository.findById(id).orElse(null)
    }

    fun getAllCreatedUserMails(user: User): List<Mail> {
        return mailRepository.findAllBySender(user).toList().filter { mail -> mail.status == MailStatus.DRAFT }
    }
    fun getAllSentUserMails(user: User): List<Mail> {
        return mailRepository.findAllBySender(user).toList().filter { mail -> mail.status == MailStatus.SENT }
    }

    @Transactional
    fun deleteMail(mail: Mail) {
        mail.attachments.map { file -> fileStorageService.deleteFile(file.path) }
        val records = mailRecordService.getMailRecordByMailId(mail.id!!)
        records.forEach { record -> mailRecordService.deleteMailRecord(record.id!!) }
        mailRepository.delete(mail)
    }

    @Transactional
    fun sendMail(mail: Mail): Mail {
        val success = smtpService.sendEmail(mail)
        if(success) {
            mail.status = MailStatus.SENT
        }else{
            mail.status = MailStatus.ERROR
        }
        return mailRepository.save(mail)
    }

    @Transactional
    fun createMail(mail: MailCreate, sender: User,  attachments: List<MultipartFile>): Mail {
        val storedAttachments = attachments.mapNotNull { file -> fileStorageService.saveFile(file) }.toMutableList()

        val mailEntity = Mail(
            sender = sender,
            subject = mail.subject,
            content = mail.content,
            attachments = mutableListOf()
        )

        this.connectAttachmentsToMail(mailEntity, storedAttachments)
        val createdMail = mailRepository.save(mailEntity)

        this.createMailRecordsFromIds(createdMail, mail.toIds, mail.ccIds, mail.bccIds, mail.replyToIds)
        return createdMail
    }

    @Transactional
    fun createAndSendMail(mail: MailCreate, sender: User, attachments: List<MultipartFile>): Mail {
        val createdMail = this.createMail(mail, sender, attachments)
        return this.sendMail(createdMail)
    }


    @Transactional
    fun updateMail(id: UUID, mail: MailUpdate, attachments: List<MultipartFile>): Mail {
        val existingMail = this.getMailById(id)!!

        existingMail.subject = mail.subject
        existingMail.content = mail.content

        existingMail.attachments.map { file -> fileStorageService.deleteFile(file.path) }
        existingMail.attachments.clear()

        val storedAttachments = attachments.mapNotNull { file -> fileStorageService.saveFile(file) }.toMutableList()
        this.connectAttachmentsToMail(existingMail, storedAttachments)

        val updatedMail = mailRepository.save(existingMail)

        val records = mailRecordService.getMailRecordByMailId(updatedMail.id!!)
        records.forEach { record ->
            mailRecordService.deleteMailRecord(record.id!!)
        }
        this.createMailRecordsFromIds(updatedMail, mail.toIds, mail.ccIds, mail.bccIds, mail.replyToIds)

        return updatedMail
    }

    private fun connectAttachmentsToMail(mail: Mail, attachments:  MutableList<AttachmentDTO>) {
        attachments.forEach { att ->
            val attachment = Attachment()
            attachment.fileName = att.fileName
            attachment.mimeType = att.mimeType
            attachment.size = att.size
            attachment.path = att.path
            mail.addAttachment(attachment)
        }
    }

    private fun createMailRecordsFromIds(
        mail: Mail,
        toIds: List<UUID>,
        ccIds: List<UUID>,
        bccIds: List<UUID>,
        replyToIds: List<UUID>)
    {
        toIds.forEach { id -> mailRecordService.createMailRecord(CreateMailRecord(
            mail = mail,
            receiver = userService.getUserById(id) ?: throw ResourceNotFoundException("Receiver not found"),
            mailType = MailType.TO
        ))}

        ccIds.forEach { id -> mailRecordService.createMailRecord(CreateMailRecord(
            mail = mail,
            receiver = userService.getUserById(id) ?: throw ResourceNotFoundException("Receiver not found"),
            mailType = MailType.CC
        ))}

        bccIds.forEach { id -> mailRecordService.createMailRecord(CreateMailRecord(
            mail = mail,
            receiver = userService.getUserById(id) ?: throw ResourceNotFoundException("Receiver not found"),
            mailType = MailType.BCC
        ))}

        replyToIds.forEach { id -> mailRecordService.createMailRecord(CreateMailRecord(
            mail = mail,
            receiver = userService.getUserById(id) ?: throw ResourceNotFoundException("Receiver not found"),
            mailType = MailType.REPLY_TO
        ))}
    }

}