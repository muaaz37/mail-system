package de.thm.mni.backend.mail.dto

import de.thm.mni.backend.mail.validation.AtLeastOneRecipient
import jakarta.validation.constraints.Size
import java.util.UUID


@AtLeastOneRecipient
data class MailRequest(
    @field:Size(min = 1, max = 20, message = "Subject must be between 1 and 20 characters")
    val subject: String,
    @field:Size(min = 1, max = 500, message = "Content must be between 1 and 500 characters")
    val content: String,
    val toIds: MutableList<UUID>,
    val ccIds: MutableList<UUID>,
    val bccIds: MutableList<UUID>,
    val replyToIds: MutableList<UUID>
)


fun MailRequest.toMailCreate(): MailCreate {
    return MailCreate(
        subject = this.subject,
        content = this.content,
        toIds = this.toIds,
        ccIds = this.ccIds,
        bccIds = this.bccIds,
        replyToIds = this.replyToIds
    )
}

fun MailRequest.toMailUpdate(): MailUpdate {
    return MailUpdate(
        subject = this.subject,
        content = this.content,
        toIds = this.toIds,
        ccIds = this.ccIds,
        bccIds = this.bccIds,
        replyToIds = this.replyToIds
    )
}