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


// This service is responsible for sending real emails via SMTP.
@Service
class SMTPService(
    // Spring component for sending emails
    private val mailSender: JavaMailSender,

    // Used to load recipients (TO, CC, BCC, REPLY_TO) for a mail
    private val mailRecordService: MailRecordService,

    // Used to load stored attachment files
    private val fileStorageService: FileStorageService,

    // Contains the configured support sender address
    private val supportMailProperties: SupportMailProperties
) {
    // Sends one mail object as a real email.
    // Returns true if sending worked, otherwise false.
    fun sendEmail(mail: Mail): Boolean {
        return try {
            // Load all recipient records for this mail
            val records = mailRecordService.getMailRecordByMailId(mail.id!!)

            // Create a MIME email message
            val mimeMessage = mailSender.createMimeMessage()

            // Helper for setting subject, content, recipients, attachments
            val helper = MimeMessageHelper(mimeMessage, true, "UTF-8")

            // Set the sender address from configuration
            helper.setFrom(InternetAddress(supportMailProperties.address))

            // Set subject and plain text content
            helper.setSubject(mail.subject)
            helper.setText(mail.content, false)

            // Collect all TO recipients
            val to = records
                .filter { it.type == MailType.TO }
                .mapNotNull { it.user?.email }
                .toTypedArray()

            // Collect all CC recipients
            val cc = records
                .filter { it.type == MailType.CC }
                .mapNotNull { it.user?.email }
                .toTypedArray()

            // Collect all BCC recipients
            val bcc = records
                .filter { it.type == MailType.BCC }
                .mapNotNull { it.user?.email }
                .toTypedArray()

            // Take the first REPLY_TO address if one exists
            val replyTo = records
                .filter { it.type == MailType.REPLY_TO }
                .mapNotNull { it.user?.email }
                .firstOrNull()

            // Only set recipient fields if they are not empty
            if (to.isNotEmpty()) helper.setTo(to)
            if (cc.isNotEmpty()) helper.setCc(cc)
            if (bcc.isNotEmpty()) helper.setBcc(bcc)
            if (replyTo != null) helper.setReplyTo(replyTo)

            // Add all stored attachments to the email
            mail.attachments.forEach { attachment ->
                val resource: Resource = fileStorageService.load(attachment.path)
                helper.addAttachment(attachment.fileName ?: attachment.path, resource)
            }

            // Send the mail
            mailSender.send(mimeMessage)
            true
        } catch (ex: Exception) {
            // If anything fails, return false
            false
        }
    }
}