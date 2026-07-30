package de.thm.mni.backend.storage

import de.thm.mni.backend.attachment.dto.AttachmentDTO
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

/**
 * Service facade for storing, loading and deleting mail attachments.
 */
@Service
class FileStorageService(private val attachmentStorage: AttachmentStorage) {
    /**
     * Stores a user-uploaded multipart file.
     */
    fun saveFile(file: MultipartFile): AttachmentDTO? {
        return attachmentStorage.save(file)
    }

    /**
     * Stores attachment bytes imported from an external mail.
     */
    fun saveFile(fileName: String?, mimeType: String?, bytes: ByteArray): AttachmentDTO? {
        return attachmentStorage.save(fileName, mimeType, bytes)
    }

    /**
     * Deletes one stored attachment file.
     */
    fun deleteFile(objectKey: String?) {
        val storedObjectKey = objectKey ?: throw FileStorageException("Object key is required.")
        attachmentStorage.delete(storedObjectKey)
    }

    /**
     * Loads a stored attachment for download or email sending.
     */
    fun load(objectKey: String): StoredAttachmentObject {
        return attachmentStorage.load(objectKey)
    }
}
