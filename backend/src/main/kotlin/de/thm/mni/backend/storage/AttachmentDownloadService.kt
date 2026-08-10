package de.thm.mni.backend.storage

import de.thm.mni.backend.attachment.Attachment
import de.thm.mni.backend.attachment.AttachmentRepository
import de.thm.mni.backend.error.ResourceNotFoundException
import de.thm.mni.backend.mail.MailAccessService
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service

/**
 * Loads attachments and enforces mail visibility before stored content is returned.
 */
@Service
class AttachmentDownloadService(
    private val attachmentRepository: AttachmentRepository,
    private val fileStorageService: FileStorageService,
    private val mailAccessService: MailAccessService
) {
    /**
     * Loads an attachment and its stored content after ensuring that the authenticated user has access to the mail.
     *
     * @param objectKey The object key of the attachment to load.
     * @param jwt The JWT of the authenticated user.
     * @return An [AuthorizedAttachment] containing the attachment metadata and stored content.
     */
    fun loadAuthorized(objectKey: String, jwt: Jwt): AuthorizedAttachment {
        val attachment = attachmentRepository.findByPath(objectKey)
            ?: throw ResourceNotFoundException("Attachment not found")
        val mail = attachment.mail ?: throw ResourceNotFoundException("Attachment not found")
        val user = mailAccessService.authenticatedUser(jwt)
        mailAccessService.ensureMailVisible(mail, user)

        return AuthorizedAttachment(
            attachment = attachment,
            storedObject = fileStorageService.load(objectKey)
        )
    }
}

/** Attachment metadata and content after successful authorization. */
data class AuthorizedAttachment(
    val attachment: Attachment,
    val storedObject: StoredAttachmentObject
)
