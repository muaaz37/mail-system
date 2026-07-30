package de.thm.mni.backend.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration values used for connecting to the IMAP support mailbox.
 */
@ConfigurationProperties(prefix = "mail.imap")
class MailImapProperties {
    lateinit var host: String
    var port: Int = DEFAULT_IMAP_PORT
    lateinit var username: String
    lateinit var password: String
    var protocol: String = "imaps"
    var folder: String = "INBOX"

    private companion object {
        const val DEFAULT_IMAP_PORT = 993
    }
}
