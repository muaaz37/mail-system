package de.thm.mni.backend.user.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Size
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Request payload for updating a user's public profile data.
 */
@Schema(description = "Request payload for updating the authenticated user's public profile.")
data class UserUpdate(
    @field:Schema(description = "User's first name.", example = "Anna")
    @field:Size(min = 1, message = "First name must not be empty")
    val firstName: String,
    @field:Schema(description = "User's last name.", example = "Schmidt")
    @field:Size(min = 1, message = "Last name must not be empty")
    val lastName: String,
    @field:Schema(description = "Unique email address used for login.", example = "anna.schmidt@example.com")
    @field:Email(message = "Email should be valid")
    val email: String
)
