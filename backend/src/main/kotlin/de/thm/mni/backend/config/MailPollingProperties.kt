package de.thm.mni.backend.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration values for the scheduled mail polling interval.
 */
@ConfigurationProperties(prefix = "mail.polling")
class MailPollingProperties {
    var intervalMs: Long = DEFAULT_INTERVAL_MS

    private companion object {
        const val DEFAULT_INTERVAL_MS = 60_000L
    }
}
