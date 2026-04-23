package de.thm.mni.backend.smtp

import de.thm.mni.backend.mail.Mail
import de.thm.mni.backend.mail.enums.MailStatus
import org.springframework.stereotype.Service


@Service
class SMTPService {
    fun sendEmail(mail: Mail): Boolean {
        return true
    }
}