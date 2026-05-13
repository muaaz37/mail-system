package de.thm.mni.backend.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "mail.polling")
class MailPollingProperties {
    var intervalMs: Long = 60000
}