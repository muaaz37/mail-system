package de.thm.mni.backend.mail

import de.thm.mni.backend.attachment.Attachment
import de.thm.mni.backend.storage.FileStorageService
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile

/**
 * Connects uploaded or imported files with mail entities.
 */
@Component
class MailAttachmentHandler(private val fileStorageService: FileStorageService) {
    /**
     * Stores uploaded files and attaches their metadata to a mail.
     */
    fun addUploadedAttachments(mail: Mail, attachments: List<MultipartFile>) {
        attachments
            .mapNotNull { file -> fileStorageService.saveFile(file) }
            .forEach { attachmentDto ->
                val attachment = Attachment()
                attachment.fileName = attachmentDto.fileName
                attachment.mimeType = attachmentDto.mimeType
                attachment.size = attachmentDto.size
                attachment.path = attachmentDto.path
                mail.addAttachment(attachment)
            }
    }

    /**
     * Replaces existing mail attachments with a new uploaded attachment list.
     */
    fun replaceAttachments(mail: Mail, attachments: List<MultipartFile>) {
        deleteAttachments(mail)
        mail.attachments.clear()
        addUploadedAttachments(mail, attachments)
    }

    /**
     * Deletes stored files for all attachments currently linked to a mail.
     */
    fun deleteAttachments(mail: Mail) {
        mail.attachments.forEach { file -> fileStorageService.deleteFile(file.path) }
    }
}
