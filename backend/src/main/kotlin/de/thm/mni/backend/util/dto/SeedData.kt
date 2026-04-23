package de.thm.mni.backend.util.dto

data class SeedData(
    val users: List<CreateSeedUser>,
    val mails: List<CreateSeedMail>
)

