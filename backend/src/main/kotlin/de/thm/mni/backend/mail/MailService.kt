package de.thm.mni.backend.mail

import de.thm.mni.backend.error.MailSendFailedException
import de.thm.mni.backend.error.ResourceCannotBeModifiedException
import de.thm.mni.backend.mail.dto.MailCreate
import de.thm.mni.backend.mail.dto.MailPayload
import de.thm.mni.backend.mail.dto.MailUpdate
import de.thm.mni.backend.mail.enums.MailDeliveryMode
import de.thm.mni.backend.mail.enums.MailStatus
import de.thm.mni.backend.mailrecord.MailRecordService
import de.thm.mni.backend.smtp.SMTPService
import de.thm.mni.backend.ticket.SupportTicketLifecycleService
import de.thm.mni.backend.user.User
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

/**
 * Coordinates mail creation, updates, sending and deletion across repositories and adapters.
 */
@Service
class MailService(
    private val mailRepository: MailRepository,
    private val smtpService: SMTPService,
    private val mailRecordService: MailRecordService,
    private val mailRecipientValidator: MailRecipientValidator,
    private val mailAttachmentHandler: MailAttachmentHandler,
    private val supportReplyService: SupportReplyService,
    private val supportTicketLifecycleService: SupportTicketLifecycleService
) {
    /**
     * Loads one mail by its identifier without applying authorization rules.
     */
    fun getMailById(id: UUID): Mail? {
        return mailRepository.findById(id).orElse(null)
    }

    /**
     * Returns all draft mails created by the given user.
     */
    fun getAllCreatedUserMails(user: User): List<Mail> {
        return mailRepository.findAllBySender(user).toList().filter { mail -> mail.status == MailStatus.DRAFT }
    }

    /**
     * Returns all sent mails created by the given user.
     */
    fun getAllSentUserMails(user: User): List<Mail> {
        return mailRepository.findAllBySender(user)
            .filter { mail -> mail.status == MailStatus.SENT }
            .sortedByDescending { mail -> mail.sentAt ?: mail.createdAt }
    }

    /**
     * Returns internal incoming mails plus imported support mails visible to all registered users.
     */
    fun getIncomingMailsForUser(userId: UUID): List<Mail> {
        val internalIncomingMails = mailRecordService.getAllIncomingMailsForUser(userId)
        val importedSupportMails = mailRepository.findAllByStatus(MailStatus.RECEIVED)
            .filter { mail -> mail.sender == null }
        return (internalIncomingMails + importedSupportMails).distinctBy { mail -> mail.id }
    }

    /**
     * Deletes a mail with its stored attachments and internal recipient records.
     */
    @Transactional
    fun deleteMail(mail: Mail) {
        mailAttachmentHandler.deleteAttachments(mail)
        val records = mailRecordService.getMailRecordByMailId(mail.id!!)
        records.forEach { record -> mailRecordService.deleteMailRecord(record.id!!) }
        mailRepository.delete(mail)
    }

    /**
     * Sends a mail through the correct delivery channel and stores the final status.
     */
    @Transactional
    fun sendMail(mail: Mail): Mail {
        if (mail.status != MailStatus.DRAFT) {
            throw ResourceCannotBeModifiedException("Only draft mails can be sent")
        }

        if (mail.deliveryMode == MailDeliveryMode.EXTERNAL && !smtpService.sendEmail(mail)) {
            throw MailSendFailedException("Mail could not be sent. The draft was kept for retry.")
        }

        mail.status = MailStatus.SENT
        val sentMail = mailRepository.save(mail)
        // Internal messages stay regular application mails. Only external support
        // replies participate in the support-ticket lifecycle.
        if (sentMail.deliveryMode == MailDeliveryMode.EXTERNAL) {
            supportTicketLifecycleService.markWaitingForCustomer(sentMail)
        }
        return sentMail
    }

    /**
     * Creates a draft mail with recipients, support reply context and attachments.
     */
    @Transactional
    fun createMail(mail: MailCreate, sender: User, attachments: List<MultipartFile>): Mail {
        validateMail(mail, sender)
        val mailEntity = Mail(
            sender = sender,
            subject = mail.subject,
            content = mail.content,
            attachments = mutableListOf()
        ).applyExternalFields(mail)

        supportReplyService.applyReplyContext(mailEntity, mail.replyToMailId)
        mailAttachmentHandler.addUploadedAttachments(mailEntity, attachments)
        val createdMail = mailRepository.save(mailEntity)
        createInternalRecords(createdMail, mail)
        return createdMail
    }

    /**
     * Creates a new mail and immediately sends it.
     */
    @Transactional
    fun createAndSendMail(mail: MailCreate, sender: User, attachments: List<MultipartFile>): Mail {
        val createdMail = createMail(mail, sender, attachments)
        return sendMail(createdMail)
    }

    /**
     * Updates a draft mail and rebuilds its recipient and attachment relations.
     */
    @Transactional
    fun updateMail(id: UUID, mail: MailUpdate, attachments: List<MultipartFile>): Mail {
        val existingMail = getMailById(id)!!
        validateMail(mail, existingMail.sender!!)
        existingMail.subject = mail.subject
        existingMail.content = mail.content
        existingMail.applyExternalFields(mail)
        supportReplyService.applyReplyContext(existingMail, mail.replyToMailId)
        mailAttachmentHandler.replaceAttachments(existingMail, attachments)

        val updatedMail = mailRepository.save(existingMail)
        val records = mailRecordService.getMailRecordByMailId(updatedMail.id!!)
        records.forEach { record -> mailRecordService.deleteMailRecord(record.id!!) }
        createInternalRecords(updatedMail, mail)
        return updatedMail
    }

    /**
     * Validates that recipients are compatible with the selected delivery mode.
     */
    private fun validateMail(mail: MailPayload, sender: User) {
        mailRecipientValidator.validate(
            deliveryMode = mail.deliveryMode,
            internalRecipientIds = mail.toIds + mail.ccIds + mail.bccIds + mail.replyToIds,
            externalRecipients = mail.externalTo + mail.externalCc + mail.externalBcc + mail.externalReplyTo,
            sender = sender
        )
    }

    /**
     * Creates internal recipient records only for mails delivered inside the application.
     */
    private fun createInternalRecords(mailEntity: Mail, mail: MailPayload) {
        if (mailEntity.deliveryMode != MailDeliveryMode.INTERNAL) {
            return
        }
        mailRecordService.createMailRecordsFromIds(
            mail = mailEntity,
            toIds = mail.toIds,
            ccIds = mail.ccIds,
            bccIds = mail.bccIds,
            replyToIds = mail.replyToIds
        )
    }

}

/**
 * Copies external recipient fields from an API payload to a mail entity.
 */
private fun Mail.applyExternalFields(mail: MailPayload): Mail {
    deliveryMode = mail.deliveryMode
    externalTo = mail.externalTo.toRecipientString()
    externalCc = mail.externalCc.toRecipientString()
    externalBcc = mail.externalBcc.toRecipientString()
    externalReplyTo = mail.externalReplyTo.toRecipientString()
    return this
}
