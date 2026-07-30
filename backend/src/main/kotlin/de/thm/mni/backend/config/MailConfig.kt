package de.thm.mni.backend.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Registers typed mail-related configuration properties.
 */
@Configuration
@EnableConfigurationProperties(
    MailImapProperties::class,
    SupportMailProperties::class,
    MailPollingProperties::class
)
class MailConfig
