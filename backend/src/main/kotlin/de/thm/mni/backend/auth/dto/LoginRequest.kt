package de.thm.mni.backend.auth.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Size
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Login request payload with email and password credentials.
 */
@Schema(description = "Credentials used to obtain a JWT bearer token.")
data class LoginRequest(
    @field:Schema(description = "Registered email address.", example = "aallanson@example.com")
    @field:Email(message = "Email should be valid")
    val email: String,
    @field:Schema(description = "Account password. The example matches the local seed user.", example = "123456")
    @field:Size(min = 6, message = "Password must be at least 6 characters long")
    val password: String
)
