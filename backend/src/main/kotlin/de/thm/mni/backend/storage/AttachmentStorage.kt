package de.thm.mni.backend.storage

import de.thm.mni.backend.attachment.dto.AttachmentDTO
import org.springframework.core.io.Resource
import org.springframework.web.multipart.MultipartFile

/**
 * Storage boundary for mail attachments independent of the concrete backend.
 */
interface AttachmentStorage {
    fun save(file: MultipartFile): AttachmentDTO?
    fun save(fileName: String?, mimeType: String?, bytes: ByteArray): AttachmentDTO?
    fun delete(objectKey: String)
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
