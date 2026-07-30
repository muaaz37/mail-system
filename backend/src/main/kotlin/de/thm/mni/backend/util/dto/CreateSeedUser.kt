package de.thm.mni.backend.util.dto


/**
 * Seed-data model for one initial user account.
 */
data class CreateSeedUser(
    val firstName: String,
    val lastName: String,
    val email: String,
    val password: String
)
