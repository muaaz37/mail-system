package de.thm.mni.backend.mail.dto

import java.util.UUID

data class MailUpdate(
    val subject: String,
    val content: String,
    val toIds: MutableList<UUID>,
    val ccIds: MutableList<UUID>,
    val bccIds: MutableList<UUID>,
    val replyToIds: MutableList<UUID>
)
