package de.thm.mni.backend.imap

import jakarta.mail.Flags
import jakarta.mail.Folder
import jakarta.mail.Message
import jakarta.mail.Multipart
import jakarta.mail.Part
import jakarta.mail.Session
import jakarta.mail.Store
import jakarta.mail.search.FlagTerm
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.Properties
import de.thm.mni.backend.mail.MailService

@Service
class IMAPService(
    private val mailService: MailService,
    @Value("\${mail.imap.host}") private val host: String,
    @Value("\${mail.imap.port}") private val port: Int,
    @Value("\${mail.imap.username}") private val username: String,
    @Value("\${mail.imap.password}") private val password: String,
    @Value("\${mail.imap.protocol}") private val protocol: String,
    @Value("\${mail.imap.folder}") private val folderName: String,
) {

    @Scheduled(fixedDelayString = "\${mail.polling.interval-ms}")
    fun fetchIncomingMails() {
        println("IMAP: checking unread mails...")

        val properties = Properties().apply {
            put("mail.store.protocol", protocol)
            put("mail.$protocol.host", host)
            put("mail.$protocol.port", port.toString())
            put("mail.$protocol.ssl.enable", "true")
        }

        var store: Store? = null
        var inbox: Folder? = null

        try {
            val session = Session.getInstance(properties)
            store = session.getStore(protocol)
            store.connect(host, port, username, password)

            inbox = store.getFolder(folderName)
            inbox.open(Folder.READ_WRITE)

            val unreadMessages = inbox.search(
                FlagTerm(Flags(Flags.Flag.SEEN), false)
            )

            println("IMAP: unread mails found = ${unreadMessages.size}")

            unreadMessages.take(5).forEach { message ->
                println("----- UNREAD MAIL -----")
                println("Subject: ${message.subject}")
                println("From: ${message.from?.joinToString()}")
                println("Sent date: ${message.sentDate}")
                println("Message-ID: ${message.getHeader("Message-ID")?.firstOrNull()}")
                println("Body: ${extractBody(message)?.take(500)}")

                val senderEmail = message.from
                    ?.firstOrNull()
                    ?.toString()
                    ?.substringAfter("<")
                    ?.substringBefore(">")
                    ?: "unknown@example.com"

                val subject = message.subject ?: "(No Subject)"
                val body = extractBody(message) ?: ""
                val messageId = message.getHeader("Message-ID")?.firstOrNull()

                val savedMail = mailService.createIncomingMailFromImap(
                    senderEmail = senderEmail,
                    subject = subject,
                    content = body,
                    messageId = messageId
                )

                if (savedMail != null) {
                    println("IMAP: mail saved to database: ${savedMail.id}")

                    message.setFlag(Flags.Flag.SEEN, true)
                } else {
                    println("IMAP: mail already exists, skipped")
                }

            }

        } catch (e: Exception) {
            println("IMAP ERROR: ${e.message}")
        } finally {
            inbox?.close(false)
            store?.close()
        }
    }

    private fun extractBody(part: Part): String? {
        return when {
            part.isMimeType("text/plain") -> {
                if (Part.ATTACHMENT.equals(part.disposition, ignoreCase = true)) null
                else part.content as? String
            }

            part.isMimeType("text/html") -> {
                if (Part.ATTACHMENT.equals(part.disposition, ignoreCase = true)) null
                else part.content as? String
            }

            part.isMimeType("multipart/*") -> {
                val multipart = part.content as Multipart

                for (i in 0 until multipart.count) {
                    val bodyPart = multipart.getBodyPart(i)

                    if (Part.ATTACHMENT.equals(bodyPart.disposition, ignoreCase = true)) {
                        continue
                    }

                    val body = extractBody(bodyPart)
                    if (!body.isNullOrBlank()) {
                        return body
                    }
                }

                null
            }

            else -> null
        }
    }
}