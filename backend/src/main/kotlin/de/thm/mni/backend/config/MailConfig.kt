package de.thm.mni.backend.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(
    MailImapProperties::class,
    SupportMailProperties::class,
    MailPollingProperties::class
)
class MailConfig