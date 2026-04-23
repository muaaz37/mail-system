package de.thm.mni.backend.mail_record.dto

import de.thm.mni.backend.mail.Mail
import de.thm.mni.backend.mail.enums.MailType
import de.thm.mni.backend.user.User

data class CreateMailRecord(
    val mail: Mail,
    val receiver: User,
    val mailType: MailType,
)
