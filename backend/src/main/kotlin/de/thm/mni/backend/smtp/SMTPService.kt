package de.thm.mni.backend.smtp

import de.thm.mni.backend.config.SupportMailProperties
import de.thm.mni.backend.mail.Mail
import de.thm.mni.backend.mail.toRecipientList
import de.thm.mni.backend.storage.FileStorageException
import de.thm.mni.backend.storage.FileStorageService
import jakarta.mail.MessagingException
import jakarta.mail.internet.InternetAddress
import org.slf4j.LoggerFactory
import org.springframework.mail.MailException
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service

/**
 * Sends outgoing support emails through the configured SMTP server.
 */
@Service
class SMTPService(
    private val mailSender: JavaMailSender,
    private val fileStorageService: FileStorageService,
    private val supportMailProperties: SupportMailProperties
) {
    private val logger = LoggerFactory.getLogger(SMTPService::class.java)

    /**
     * Builds a MIME message from a stored mail and sends it with attachments.
     */
    fun sendEmail(mail: Mail): Boolean {
        return try {
            val mimeMessage = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(mimeMessage, true, "UTF-8")

            mimeMessage.setHeader(APP_ORIGIN_HEADER, APP_ORIGIN_VALUE)
            mail.id?.let { id -> mimeMessage.setHeader(APP_MAIL_ID_HEADER, id.toString()) }

            helper.setFrom(InternetAddress(supportMailProperties.address))
            helper.setSubject(mail.subject)
            helper.setText(mail.content, false)
            setRecipients(helper, mail)
            addAttachments(helper, mail)

            mailSender.send(mimeMessage)
            true
        } catch (ex: MessagingException) {
            logSendFailure(mail, ex)
            false
        } catch (ex: MailException) {
            logSendFailure(mail, ex)
            false
        } catch (ex: FileStorageException) {
            logSendFailure(mail, ex)
            false
        }
    }

    /**
     * Applies To, Cc, Bcc and Reply-To headers from the stored external fields.
     */
    private fun setRecipients(helper: MimeMessageHelper, mail: Mail) {
        val to = mail.externalTo.toRecipientList().toTypedArray()
        val cc = mail.externalCc.toRecipientList().toTypedArray()
        val bcc = mail.externalBcc.toRecipientList().toTypedArray()
        val replyTo = mail.externalReplyTo.toRecipientList().firstOrNull()

        if (to.isNotEmpty()) helper.setTo(to)
        if (cc.isNotEmpty()) helper.setCc(cc)
        if (bcc.isNotEmpty()) helper.setBcc(bcc)
        if (replyTo != null) helper.setReplyTo(replyTo)
    }

    /**
     * Adds stored mail attachments to the outgoing MIME message.
     */
    private fun addAttachments(helper: MimeMessageHelper, mail: Mail) {
        mail.attachments.forEach { attachment ->
            val storedObject = fileStorageService.load(attachment.path)
            helper.addAttachment(attachment.fileName ?: attachment.path, storedObject.resource)
        }
    }

    private fun logSendFailure(mail: Mail, ex: Exception) {
        logger.error("Failed to send mail with id={}, subject='{}'", mail.id, mail.subject, ex)
    }

    companion object {
        const val APP_ORIGIN_HEADER = "X-Mail-System-Origin"
        const val APP_ORIGIN_VALUE = "mail-system"
        const val APP_MAIL_ID_HEADER = "X-Mail-System-Mail-Id"
    }
}
