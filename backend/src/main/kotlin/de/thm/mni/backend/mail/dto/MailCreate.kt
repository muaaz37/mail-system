package de.thm.mni.backend.mail.dto

import java.util.UUID

data class MailCreate(
    val subject: String,
    val content: String,
    val toIds: MutableList<UUID>,
    val ccIds: MutableList<UUID>,
    val bccIds: MutableList<UUID>,
    val replyToIds: MutableList<UUID>
)
