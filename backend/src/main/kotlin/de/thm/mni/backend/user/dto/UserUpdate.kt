package de.thm.mni.backend.user.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Size

data class UserUpdate(
    @field:Size(min = 1, message = "First name must not be empty")
    val firstName: String,
    @field:Size(min = 1, message = "Last name must not be empty")
    val lastName: String,
    @field:Email(message = "Email should be valid")
    val email: String
)
