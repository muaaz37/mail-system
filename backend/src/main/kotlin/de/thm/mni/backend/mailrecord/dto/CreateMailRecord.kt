package de.thm.mni.backend.mailrecord.dto

import de.thm.mni.backend.mail.Mail
import de.thm.mni.backend.mail.enums.MailType
import de.thm.mni.backend.user.User

/**
 * Service-layer payload for creating an internal mail recipient record.
 */
data class CreateMailRecord(
    val mail: Mail,
    val receiver: User,
    val mailType: MailType,
)
