package de.thm.mni.backend.auth.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Size

data class LoginRequest(
    @field:Email(message = "Email should be valid")
    val email: String,
    @field:Size(min = 6, message = "Password must be at least 6 characters long")
    val password: String
)
