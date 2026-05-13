package de.thm.mni.backend.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "support.mail")
class SupportMailProperties {
    lateinit var address: String
}