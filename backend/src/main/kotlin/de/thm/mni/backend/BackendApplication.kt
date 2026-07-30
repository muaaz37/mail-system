package de.thm.mni.backend

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Contact
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityScheme
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
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
        description = "REST API for authentication, users, mails, support replies and attachments.",
        contact = Contact(name = "Mail Support System")
    ),
    security = [SecurityRequirement(name = "bearerAuth")]
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT"
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
