package de.thm.mni.backend.util

import de.thm.mni.backend.mail.Mail
import de.thm.mni.backend.mail.MailRepository
import de.thm.mni.backend.mail.enums.MailStatus
import de.thm.mni.backend.mail.enums.MailType
import de.thm.mni.backend.mail_record.MailRecord
import de.thm.mni.backend.mail_record.MailRecordRepository
import de.thm.mni.backend.user.User
import de.thm.mni.backend.user.UserRepository
import de.thm.mni.backend.util.dto.SeedData
import org.springframework.boot.CommandLineRunner
import org.springframework.core.io.ClassPathResource
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import tools.jackson.core.type.TypeReference
import kotlin.collections.forEach


@Component
class DatabaseInitializer(
    private val userRepository: UserRepository,
    private val mailRepository: MailRepository,
    private val mailRecordRepository: MailRecordRepository,
    private val passwordEncoder: PasswordEncoder
): CommandLineRunner {
    override fun run(vararg args: String) {
        try {
            val resource = ClassPathResource("data.json")

            val objectMapper = ObjectMapper()
            val jsonData: SeedData = objectMapper.readValue(
                resource.inputStream,
                object : TypeReference<SeedData>() {}
            )
            val usersDto = jsonData.users
            val mailsDto = jsonData.mails

            val usersToSave = usersDto.map { dto ->
                User(
                    firstName = dto.firstName,
                    lastName = dto.lastName,
                    email = dto.email,
                    password = passwordEncoder.encode(dto.password).toString()
                )
            }
            userRepository.saveAll(usersToSave)

            mailsDto.forEach { dto ->
                val mail = Mail(
                    sender = userRepository.findByEmail((dto.senderEmail))!!,
                    subject = dto.subject,
                    content = dto.content,
                    attachments = mutableListOf()
                )
                if(dto.status == MailStatus.SENT) {
                    mail.status = MailStatus.SENT
                }
                val createdMail = mailRepository.save(mail)
                this.createMailRecords(createdMail, dto.toEmails, dto.ccEmails, dto.bccEmails, dto.replyToEmails)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun createMailRecords(
        mail: Mail,
        to: List<String>,
        cc: List<String>,
        bcc: List<String>,
        replyTo: List<String>)
    {
        to.forEach { addr -> mailRecordRepository.save(MailRecord(
            mail = mail,
            user = userRepository.findUserByEmail(addr)!!,
            type = MailType.TO
        ))}

        cc.forEach { addr -> mailRecordRepository.save(MailRecord(
            mail = mail,
            user = userRepository.findUserByEmail(addr)!!,
            type = MailType.CC
        ))}

        bcc.forEach { addr -> mailRecordRepository.save(MailRecord(
            mail = mail,
            user = userRepository.findUserByEmail(addr)!!,
            type = MailType.BCC
        ))}

        replyTo.forEach { addr -> mailRecordRepository.save(MailRecord(
            mail = mail,
            user = userRepository.findUserByEmail(addr)!!,
            type = MailType.REPLY_TO
        ))}
    }
}