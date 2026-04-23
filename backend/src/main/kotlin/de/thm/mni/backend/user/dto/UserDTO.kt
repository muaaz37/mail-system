package de.thm.mni.backend.user.dto

import de.thm.mni.backend.user.User
import java.util.UUID

data class UserDTO(
    val id: UUID?,
    val firstName: String,
    val lastName: String,
    val email: String
)


fun User.toDTO() = UserDTO(
    id = this.id,
    firstName = this.firstName,
    lastName = this.lastName,
    email = this.email
)