package de.thm.mni.backend.auth.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Size

data class RegisterRequest(
    @field:Size(min = 1, message = "First name must not be empty")
    val firstName: String,
    @field:Size(min = 1, message = "Last name must not be empty")
    val lastName: String,
    @field:Email(message = "Email should be valid")
    val email: String,
    @field:Size(min = 6, message = "Password must be at least 6 characters long")
    val password: String
)
