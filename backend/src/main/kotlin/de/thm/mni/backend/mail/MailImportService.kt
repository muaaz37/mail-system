package de.thm.mni.backend.mail

import de.thm.mni.backend.attachment.Attachment
import de.thm.mni.backend.attachment.dto.AttachmentDTO
import de.thm.mni.backend.config.SupportMailProperties
import de.thm.mni.backend.imap.IMAPService
import de.thm.mni.backend.imap.dto.ImapMailData
import de.thm.mni.backend.mail.enums.MailDeliveryMode
import de.thm.mni.backend.mail.enums.MailStatus
import de.thm.mni.backend.storage.FileStorageService
import de.thm.mni.backend.ticket.SupportTicketLifecycleService
import jakarta.mail.internet.InternetAddress
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.time.ZoneId

/**
 * Imports unread IMAP support mails and persists them as external received mails.
 */
@Service
class MailImportService(
    private val mailRepository: MailRepository,
    private val fileStorageService: FileStorageService,
    private val imapService: IMAPService,
    private val supportMailProperties: SupportMailProperties,
    private val supportTicketService: SupportTicketService,
    private val supportTicketLifecycleService: SupportTicketLifecycleService,
    private val transactionTemplate: TransactionTemplate
) {
    private val logger = LoggerFactory.getLogger(MailImportService::class.java)

    /**
     * Imports unread mails and only allows IMAP messages to be marked as seen after a successful transaction.
     */
    fun importUnreadMails(): List<Mail> {
        val importedMails = mutableListOf<Mail>()

        imapService.processUnreadMailData { imapMail ->
            val importResult = importUnreadMailInTransaction(imapMail)
            if (importResult.mail != null) {
                importedMails.add(importResult.mail)
            }

            importResult.markAsSeen
        }

        return importedMails
    }

    /**
     * Wraps one IMAP mail import in its own transaction to isolate failures per message.
     */
    private fun importUnreadMailInTransaction(imapMail: ImapMailData): ImportResult {
        return transactionTemplate.execute { importUnreadMail(imapMail) }
            ?: ImportResult(mail = null, markAsSeen = false)
    }

    /**
     * Converts one IMAP payload into a persisted support mail with attachments.
     */
    private fun importUnreadMail(imapMail: ImapMailData): ImportResult {
        val skipReason = importSkipReason(imapMail)
        if (skipReason != null) {
            logger.info("Skipping {} with Message-ID={}", skipReason, normalizeMessageId(imapMail.messageId))
            return ImportResult(mail = null, markAsSeen = true)
        }

        val senderAddress = InternetAddress.parse(imapMail.from ?: "").firstOrNull()
        val mail = Mail().apply {
            sender = null
            subject = imapMail.subject
            content = imapMail.body ?: ""
            status = MailStatus.RECEIVED
            deliveryMode = MailDeliveryMode.EXTERNAL
            externalSenderEmail = senderAddress?.address
            externalSenderName = senderAddress?.personal
            externalMessageId = normalizeMessageId(imapMail.messageId)
            externalTo = imapMail.to.ifEmpty { listOf(supportMailProperties.address) }.toRecipientString()
            externalCc = imapMail.cc.toRecipientString()
            externalReplyTo = imapMail.replyTo.toRecipientString()
            ticketNumber = supportTicketService.extractTicketNumber(imapMail.subject)
            externalSentAt = imapMail.sentDate
                ?.toInstant()
                ?.atZone(ZoneId.systemDefault())
                ?.toLocalDateTime()
        }

        val storedAttachments = imapMail.attachments
            .mapNotNull { attachment ->
                fileStorageService.saveFile(attachment.fileName, attachment.mimeType, attachment.bytes)
            }
            .toMutableList()
        connectAttachmentsToMail(mail, storedAttachments)

        val savedMail = mailRepository.save(mail)
        supportTicketLifecycleService.attachIncomingMail(savedMail)
        return ImportResult(mail = mailRepository.save(savedMail), markAsSeen = true)
    }

    /**
     * Skips app-generated mails and mails already imported by Message-ID.
     */
    private fun importSkipReason(imapMail: ImapMailData): String? {
        val externalMessageId = normalizeMessageId(imapMail.messageId)
        return when {
            imapMail.systemGenerated -> "app-generated IMAP mail"
            externalMessageId != null && mailRepository.existsByExternalMessageId(externalMessageId) -> {
                "already imported IMAP mail"
            }
            else -> null
        }
    }

    private fun normalizeMessageId(messageId: String?): String? {
        return messageId?.trim()?.takeIf { id -> id.isNotEmpty() }
    }

    /**
     * Rebuilds attachment entities from stored attachment metadata.
     */
    private fun connectAttachmentsToMail(mail: Mail, attachments: MutableList<AttachmentDTO>) {
        attachments.forEach { attachmentDto ->
            val attachment = Attachment()
            attachment.fileName = attachmentDto.fileName
            attachment.mimeType = attachmentDto.mimeType
            attachment.size = attachmentDto.size
            attachment.path = attachmentDto.path
            mail.addAttachment(attachment)
        }
    }

    private data class ImportResult(
        val mail: Mail?,
        val markAsSeen: Boolean
    )
}
