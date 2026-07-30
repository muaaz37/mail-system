package de.thm.mni.backend.storage.s3

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration for an S3-compatible attachment storage endpoint.
 */
@ConfigurationProperties(prefix = "storage.s3")
data class S3StorageProperties(
    var endpoint: String = "http://localhost:8333",
    var region: String = "us-east-1",
    var bucket: String = "mail-attachments",
    var accessKey: String = "",
    var secretKey: String = "",
    var pathStyleAccess: Boolean = true,
    var initializeBucket: Boolean = false
)
