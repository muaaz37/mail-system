package de.thm.mni.backend.util

import de.thm.mni.backend.mail.Mail
import de.thm.mni.backend.mail.MailRepository
import de.thm.mni.backend.mail.enums.MailStatus
import de.thm.mni.backend.mail.enums.MailType
import de.thm.mni.backend.mailrecord.MailRecord
import de.thm.mni.backend.mailrecord.MailRecordRepository
import de.thm.mni.backend.user.User
import de.thm.mni.backend.user.UserRepository
import de.thm.mni.backend.util.dto.SeedData
import org.springframework.boot.CommandLineRunner
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.ObjectMapper

/**
 * Loads sample users and mails from data.json when the backend starts.
 */
@Component
class DatabaseInitializer(
    private val userRepository: UserRepository,
    private val mailRepository: MailRepository,
    private val mailRecordRepository: MailRecordRepository,
    private val objectMapper: ObjectMapper
) : CommandLineRunner {
    /**
     * Reads seed data and stores the initial users, mails and recipient records.
     */
    override fun run(vararg args: String) {
        val resource = ClassPathResource("data.json")
        val jsonData: SeedData = objectMapper.readValue(
            resource.inputStream,
            object : TypeReference<SeedData>() {}
        )

        jsonData.users.forEach { dto ->
            if (!userRepository.existsUserByEmail(dto.email)) {
                userRepository.save(User(
                firstName = dto.firstName,
                lastName = dto.lastName,
                email = dto.email,
                ))
            }
        }

        if (mailRepository.count() > 0) {
            return
        }

        jsonData.mails.forEach { dto ->
            val mail = Mail(
                sender = userRepository.findUserByEmail(dto.senderEmail)!!,
                subject = dto.subject,
                content = dto.content,
                attachments = mutableListOf()
            )
            if (dto.status == MailStatus.SENT) {
                mail.status = MailStatus.SENT
            }
            val createdMail = mailRepository.save(mail)
            createMailRecords(createdMail, dto.toEmails, dto.ccEmails, dto.bccEmails, dto.replyToEmails)
        }
    }

    /**
     * Creates recipient records for all recipient lists in one seed mail.
     */
    private fun createMailRecords(
        mail: Mail,
        to: List<String>,
        cc: List<String>,
        bcc: List<String>,
        replyTo: List<String>
    ) {
        createMailRecords(mail, to, MailType.TO)
        createMailRecords(mail, cc, MailType.CC)
        createMailRecords(mail, bcc, MailType.BCC)
        createMailRecords(mail, replyTo, MailType.REPLY_TO)
    }

    /**
     * Resolves seed email addresses to users and stores records for one recipient type.
     */
    private fun createMailRecords(mail: Mail, addresses: List<String>, mailType: MailType) {
        addresses.forEach { address ->
            mailRecordRepository.save(
                MailRecord(
                    mail = mail,
                    user = userRepository.findUserByEmail(address)!!,
                    type = mailType
                )
            )
        }
    }
}
