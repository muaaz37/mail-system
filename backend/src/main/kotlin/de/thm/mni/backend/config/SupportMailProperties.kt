package de.thm.mni.backend.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration values for the shared support sender address.
 */
@ConfigurationProperties(prefix = "support.mail")
class SupportMailProperties {
    lateinit var address: String
}
