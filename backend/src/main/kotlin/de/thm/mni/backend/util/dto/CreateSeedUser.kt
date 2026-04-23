package de.thm.mni.backend.util.dto


data class CreateSeedUser(
    val firstName: String,
    val lastName: String,
    val email: String,
    val password: String
)
