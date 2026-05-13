package de.thm.mni.backend.config

import org.springframework.boot.context.properties.ConfigurationProperties


@ConfigurationProperties(prefix = "mail.imap")
class MailImapProperties {
    lateinit var host: String
    var port: Int = 993
    lateinit var username: String
    lateinit var password: String
    var protocol: String = "imaps"
    var folder: String = "INBOX"
}