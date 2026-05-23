package de.thm.mni.backend.imap

import jakarta.mail.Folder
import jakarta.mail.Session
import jakarta.mail.Store
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.Properties

@Service
class IMAPService(
    @Value("\${mail.imap.host}") private val host: String,
    @Value("\${mail.imap.port}") private val port: Int,
    @Value("\${mail.imap.username}") private val username: String,
    @Value("\${mail.imap.password}") private val password: String,
    @Value("\${mail.imap.protocol}") private val protocol: String,
    @Value("\${mail.imap.folder}") private val folderName: String,
) {

    @Scheduled(fixedDelayString = "\${mail.polling.interval-ms}")
    fun fetchIncomingMails() {
        println("IMAP: connecting to $host:$port")

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

            println("IMAP: connected successfully")
            println("IMAP: total mails = ${inbox.messageCount}")
            println("IMAP: unread mails = ${inbox.unreadMessageCount}")

        } catch (e: Exception) {
            println("IMAP ERROR: ${e.message}")
        } finally {
            inbox?.close(false)
            store?.close()
        }
    }
}