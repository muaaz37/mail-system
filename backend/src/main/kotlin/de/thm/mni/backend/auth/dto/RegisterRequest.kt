package de.thm.mni.backend.auth.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Size
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Registration request payload for creating a new user account.
 */
@Schema(description = "Request payload for creating a user account.")
data class RegisterRequest(
    @field:Schema(description = "User's first name.", example = "Nora")
    @field:Size(min = 1, message = "First name must not be empty")
    val firstName: String,
    @field:Schema(description = "User's last name.", example = "Becker")
    @field:Size(min = 1, message = "Last name must not be empty")
    val lastName: String,
    @field:Schema(description = "Email address used for login. Change it if the example account already exists.", example = "nora.becker@example.com")
    @field:Email(message = "Email should be valid")
    val email: String,
    @field:Schema(description = "Account password. Must contain at least six characters.", example = "SecurePass123!")
    @field:Size(min = 6, message = "Password must be at least 6 characters long")
    val password: String
)
