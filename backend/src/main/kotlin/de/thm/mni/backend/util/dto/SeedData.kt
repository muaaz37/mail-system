package de.thm.mni.backend.util.dto

/**
 * Root seed-data model loaded from data.json.
 */
data class SeedData(
    val users: List<CreateSeedUser>,
    val mails: List<CreateSeedMail>
)

