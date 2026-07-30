package de.thm.mni.backend.imap

import de.thm.mni.backend.config.MailImapProperties
import de.thm.mni.backend.imap.dto.ImapMailData
import de.thm.mni.backend.imap.dto.ImapMailPreview
import jakarta.mail.Flags
import jakarta.mail.Folder
import jakarta.mail.Message
import jakarta.mail.Session
import jakarta.mail.Store
import jakarta.mail.search.FlagTerm
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Properties

/**
 * Handles low-level IMAP access for unread support mailbox messages.
 */
@Service
class IMAPService(
    private val mailImapProperties: MailImapProperties,
    private val imapMessageMapper: ImapMessageMapper
) {
    private val logger = LoggerFactory.getLogger(IMAPService::class.java)

    /**
     * Opens the configured mailbox in read-write mode so processed messages can be marked as seen.
     */
    fun openInbox(): Folder {
        val properties = Properties().apply {
            put("mail.store.protocol", mailImapProperties.protocol)
            put("mail.imaps.ssl.enable", "true")
        }

        val session = Session.getInstance(properties)
        val store: Store = session.getStore(mailImapProperties.protocol)
        store.connect(
            mailImapProperties.host,
            mailImapProperties.port,
            mailImapProperties.username,
            mailImapProperties.password
        )

        return store.getFolder(mailImapProperties.folder).apply {
            open(Folder.READ_WRITE)
        }
    }

    /**
     * Returns lightweight previews of unread messages for diagnostics.
     */
    fun getUnreadMailData(): List<ImapMailPreview> {
        return withInbox { inbox ->
            findUnreadMessages(inbox).map { message -> imapMessageMapper.toPreview(message) }
        }
    }

    /**
     * Processes unread messages and marks each message as seen only after successful persistence.
     */
    fun processUnreadMailData(processMail: (ImapMailData) -> Boolean) {
        withInbox { inbox ->
            findUnreadMessages(inbox).forEach { message -> processMessage(message, processMail) }
        }
    }

    /**
     * Counts unread messages in the configured mailbox.
     */
    fun countUnreadMessages(): Int {
        return withInbox { inbox -> findUnreadMessages(inbox).size }
    }

    /**
     * Opens the mailbox for one action and always closes folder and store afterwards.
     */
    private fun <T> withInbox(action: (Folder) -> T): T {
        val inbox = openInbox()
        return try {
            action(inbox)
        } finally {
            if (inbox.isOpen) {
                inbox.close(false)
            }
            inbox.store.close()
        }
    }

    /**
     * Processes one message and restores the unread flag when mapping or persistence fails.
     */
    @Suppress("TooGenericExceptionCaught")
    private fun processMessage(message: Message, processMail: (ImapMailData) -> Boolean) {
        try {
            val mailData = imapMessageMapper.toMailData(message)
            val markAsSeen = processMail(mailData)
            message.setFlag(Flags.Flag.SEEN, markAsSeen)
        } catch (ex: Exception) {
            clearSeenFlag(message)
            logger.error("Failed to process unread IMAP message", ex)
        }
    }

    /**
     * Clears the SEEN flag after a failed import so the next polling cycle can retry.
     */
    @Suppress("TooGenericExceptionCaught")
    private fun clearSeenFlag(message: Message) {
        try {
            message.setFlag(Flags.Flag.SEEN, false)
        } catch (ex: Exception) {
            logger.warn("Failed to restore unread IMAP flag after import error", ex)
        }
    }

    /**
     * Searches the open mailbox for messages without the IMAP SEEN flag.
     */
    private fun findUnreadMessages(inbox: Folder): Array<Message> {
        return inbox.search(FlagTerm(Flags(Flags.Flag.SEEN), false))
    }
}
