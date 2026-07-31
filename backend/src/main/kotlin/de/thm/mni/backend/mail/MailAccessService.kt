package de.thm.mni.backend.mail

import de.thm.mni.backend.error.ResourceNotFoundException
import de.thm.mni.backend.mail.enums.MailStatus
import de.thm.mni.backend.mailrecord.MailRecordService
import de.thm.mni.backend.user.User
import de.thm.mni.backend.security.CurrentUserService
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Centralizes mail lookup and authorization checks for mail endpoints.
 */
@Service
class MailAccessService(
    private val mailService: MailService,
    private val mailRecordService: MailRecordService,
    private val currentUserService: CurrentUserService
) {
    /**
     * Resolves the authenticated Keycloak identity to a local profile.
     */
    fun authenticatedUser(jwt: Jwt): User = currentUserService.resolve(jwt)

    /**
     * Loads a mail or hides missing mails behind a not-found error.
     */
    fun mailOrNotFound(mailId: UUID): Mail {
        return mailService.getMailById(mailId) ?: throw ResourceNotFoundException("Mail not found")
    }

    /**
     * Allows access for senders, internal recipients and imported support mails.
     */
    fun ensureMailVisible(mail: Mail, user: User) {
        val records = mailRecordService.getMailRecordByMailId(mail.id!!)
        val isInternalSender = mail.sender?.id == user.id
        val isInternalRecipient = records.any { record -> record.user?.id == user.id }
        val isImportedSupportMail = mail.status == MailStatus.RECEIVED && mail.sender == null

        if (!isImportedSupportMail && !isInternalSender && !isInternalRecipient) {
            throw ResourceNotFoundException("Mail not found")
        }
    }

    /**
     * Allows changes only for mails owned by the authenticated sender.
     */
    fun ensureOwnedBy(mail: Mail, userId: UUID) {
        if (mail.sender?.id != userId) {
            throw ResourceNotFoundException("Mail not found")
        }
    }
}
