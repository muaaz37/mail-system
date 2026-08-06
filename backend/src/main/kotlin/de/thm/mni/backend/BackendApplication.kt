package de.thm.mni.backend

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Contact
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.info.License
import io.swagger.v3.oas.annotations.ExternalDocumentation
import io.swagger.v3.oas.annotations.security.SecurityScheme
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

/**
 * Spring Boot entry point for the backend application.
 */
@OpenAPIDefinition(
    info = Info(
        title = "Mail Support System API",
        version = "1.0.0",
        description = "API for managing users, internal and external mail, attachments, and support tickets. " +
                "User identities and credentials are managed by an external identity provider " +
                "through OpenID Connect. " +
                "Protected operations require a valid access token supplied as a JWT bearer token."
        ,
        contact = Contact(
            name = "Mail Support System",
            url = "https://github.com/muaaz37/mail-system"
        ),
        license = License(
            name = "MIT License",
            url =  "https://github.com/muaaz37/mail-system/blob/main/LICENSE"
        )
    ),
    externalDocs = ExternalDocumentation(
        description = "Project documentation",
        url = "https://github.com/muaaz37/mail-system/blob/main/README.adoc"
    ),
    tags = [
        Tag(
            name = "User",
            description = "Manage registered application users."
        ),
        Tag(
            name = "Mail",
            description = "Manage drafts, sent mails, inbox mails and support replies."
        ),
        Tag(
            name = "Support Ticket",
            description = "Manage shared support tickets and lifecycle states."
        ),
        Tag(
            name = "Attachment",
            description = "Download stored mail attachments."
        ),
        Tag(
            name = "IMAP Diagnostics",
            description = "Inspect and trigger support mailbox imports."
        )
    ]
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "OpenID Connect access token issued for this API by the configured identity provider. " +
            "Enter only the token value without the `Bearer` prefix."
)
@SpringBootApplication
@EnableScheduling
class BackendApplication

/**
 * Starts the Spring Boot backend.
 */
fun main(args: Array<String>) {
    runApplication<BackendApplication>(*args)
}
