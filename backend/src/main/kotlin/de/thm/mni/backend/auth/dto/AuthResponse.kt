package de.thm.mni.backend.auth.dto

import de.thm.mni.backend.user.dto.UserDTO

/**
 * Response returned after successful registration or login.
 */
data class AuthResponse(
    val user: UserDTO,
    val token: String,
)
