package de.thm.mni.backend.smtp

import de.thm.mni.backend.config.SupportMailProperties
import de.thm.mni.backend.mail.Mail
import de.thm.mni.backend.mail.enums.MailType
import de.thm.mni.backend.mail_record.MailRecordService
import de.thm.mni.backend.storage.FileStorageService
import jakarta.mail.internet.InternetAddress
import org.springframework.core.io.Resource
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service


@Service
class SMTPService(
    private val mailSender: JavaMailSender,
    private val mailRecordService: MailRecordService,
    private val fileStorageService: FileStorageService,
    private val supportMailProperties: SupportMailProperties
) {
    fun sendEmail(mail: Mail): Boolean {
        return try {
            val records = mailRecordService.getMailRecordByMailId(mail.id!!)
            val mimeMessage = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(mimeMessage, true, "UTF-8")

            helper.setFrom(InternetAddress(supportMailProperties.address))
            helper.setSubject(mail.subject)
            helper.setText(mail.content, false)

            val to = records
                .filter { it.type == MailType.TO }
                .mapNotNull { it.user?.email }
                .toTypedArray()

            val cc = records
                .filter { it.type == MailType.CC }
                .mapNotNull { it.user?.email }
                .toTypedArray()

            val bcc = records
                .filter { it.type == MailType.BCC }
                .mapNotNull { it.user?.email }
                .toTypedArray()
            val replyTo = records
                .filter { it.type == MailType.REPLY_TO }
                .mapNotNull { it.user?.email }
                .firstOrNull()

            if (to.isNotEmpty()) helper.setTo(to)
            if (cc.isNotEmpty()) helper.setCc(cc)
            if (bcc.isNotEmpty()) helper.setBcc(bcc)
            if (replyTo != null) helper.setReplyTo(replyTo)
            mail.attachments.forEach { attachment ->
                val resource: Resource = fileStorageService.load(attachment.path)
                helper.addAttachment(attachment.fileName ?: attachment.path, resource)
            }
            mailSender.send(mimeMessage)
            true
        } catch (ex: Exception) {
            false
        }
    }
}