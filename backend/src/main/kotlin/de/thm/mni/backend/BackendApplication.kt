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
            "Support messages are imported through IMAP and external replies are sent through SMTP. " +
            "Registration and login are public; all other operations require a JWT bearer token.",
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
        Tag(name = "Auth", description = "Register users and issue JWT bearer tokens."),
        Tag(name = "User", description = "Manage registered application users."),
        Tag(name = "Mail", description = "Manage drafts, sent mails, inbox mails and support replies."),
        Tag(name = "Support Ticket", description = "Manage shared support tickets and lifecycle states."),
        Tag(name = "Attachment", description = "Download stored mail attachments."),
        Tag(name = "IMAP Diagnostics", description = "Inspect and trigger support mailbox imports.")
    ]
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "JWT returned by `/api/register` or `/api/login`. " +
        "Use the token without the `Bearer` prefix in Swagger UI's Authorize dialog."
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
