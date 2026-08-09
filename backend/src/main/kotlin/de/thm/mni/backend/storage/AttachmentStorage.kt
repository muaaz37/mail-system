package de.thm.mni.backend.storage

import de.thm.mni.backend.attachment.dto.AttachmentDTO
import org.springframework.core.io.Resource
import org.springframework.web.multipart.MultipartFile

/**
 * Storage boundary for mail attachments independent of the concrete backend.
 */
interface AttachmentStorage {
    /**
     * Stores a multipart attachment uploaded through the REST API.
     *
     * @param file Browser-uploaded file.
     * @return Attachment metadata, or null when the upload is empty.
     */
    fun save(file: MultipartFile): AttachmentDTO?

    /**
     * Stores attachment bytes extracted from an imported mail.
     *
     * @param fileName Original file name, if provided by the mail client.
     * @param mimeType MIME type reported by the source message.
     * @param bytes Binary attachment content.
     * @return Attachment metadata, or null when the content is empty.
     */
    fun save(fileName: String?, mimeType: String?, bytes: ByteArray): AttachmentDTO?

    /**
     * Deletes the object identified by its storage key.
     *
     * @param objectKey Storage key persisted on the attachment entity.
     */
    fun delete(objectKey: String)

    /**
     * Loads the object identified by its storage key.
     *
     * @param objectKey Storage key persisted on the attachment entity.
     * @return Binary attachment object plus response metadata.
     */
    fun load(objectKey: String): StoredAttachmentObject
}

/**
 * Loaded attachment object plus response metadata.
 */
data class StoredAttachmentObject(
    val resource: Resource,
    val contentType: String?,
    val contentLength: Long?
)
