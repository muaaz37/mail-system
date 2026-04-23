package de.thm.mni.backend.auth.dto

import de.thm.mni.backend.user.dto.UserDTO

data class AuthResponse(
    val user: UserDTO,
    val token: String,
)
