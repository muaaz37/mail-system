package de.thm.mni.backend.storage.s3

import de.thm.mni.backend.attachment.dto.AttachmentDTO
import de.thm.mni.backend.storage.AttachmentStorage
import de.thm.mni.backend.storage.AttachmentContentTypes
import de.thm.mni.backend.storage.FileStorageException
import de.thm.mni.backend.storage.FileStorageObjectNotFoundException
import de.thm.mni.backend.storage.UnsupportedAttachmentTypeException
import de.thm.mni.backend.storage.StoredAttachmentObject
import org.springframework.core.io.ByteArrayResource
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import software.amazon.awssdk.core.ResponseBytes
import software.amazon.awssdk.core.exception.SdkException
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.S3Exception
import java.io.IOException
import java.nio.file.Paths
import java.util.UUID

/**
 * Stores mail attachments in an S3-compatible object store.
 */
@Component
class S3AttachmentStorage(
    private val s3Client: S3Client,
    private val properties: S3StorageProperties
) : AttachmentStorage {
    /**
     * Stores a multipart attachment uploaded through the REST API.
     *
     * @param file Browser-uploaded multipart file.
     * @return Attachment metadata, or null when the upload is empty.
     */
    override fun save(file: MultipartFile): AttachmentDTO? {
        if (file.isEmpty) {
            return null
        }

        return try {
            val bytes = file.bytes
            val detectedType = validatedUploadType(file.contentType, bytes)
            save(file.originalFilename, detectedType, bytes)
        } catch (ex: IOException) {
            throw FileStorageException("Failed to read uploaded file content.", ex)
        }
    }

    /**
     * Validates that browser uploads use one of the allowed previewable formats.
     */
    private fun validatedUploadType(claimedContentType: String?, bytes: ByteArray): String {
        val detectedType = AttachmentContentTypes.detect(bytes)
            ?: throw UnsupportedAttachmentTypeException(
                "Only PDF, PNG, JPEG, GIF and WebP attachments are allowed."
            )
        val normalizedClaim = AttachmentContentTypes.normalizeClaimedType(claimedContentType)
        if (normalizedClaim != detectedType) {
            throw UnsupportedAttachmentTypeException(
                "The attachment content does not match its declared media type."
            )
        }
        return detectedType
    }

    /**
     * Stores an attachment byte array under a generated object key.
     *
     * @param fileName Original file name used only as metadata and for the object-key extension.
     * @param mimeType MIME type reported by the source message.
     * @param bytes Binary attachment content.
     * @return Attachment metadata, or null when the content is empty.
     */
    override fun save(fileName: String?, mimeType: String?, bytes: ByteArray): AttachmentDTO? {
        if (bytes.isEmpty()) {
            return null
        }

        val safeFilename = sanitizeFilename(fileName)
        val objectKey = UUID.randomUUID().toString() + fileExtension(safeFilename)
        // Imported email attachments may contain other formats. Store those as
        // binary downloads instead of trusting externally supplied MIME data.
        val contentType = AttachmentContentTypes.detect(bytes) ?: AttachmentContentTypes.BINARY

        val request = PutObjectRequest.builder()
            .bucket(properties.bucket)
            .key(objectKey)
            .contentType(contentType)
            .contentLength(bytes.size.toLong())
            .metadata(mapOf(ORIGINAL_FILENAME_METADATA to safeFilename))
            .build()

        try {
            s3Client.putObject(request, RequestBody.fromBytes(bytes))
        } catch (ex: SdkException) {
            throw FileStorageException("Failed to store attachment in S3.", ex)
        }

        return AttachmentDTO(
            size = bytes.size.toLong(),
            fileName = safeFilename,
            mimeType = contentType,
            path = objectKey
        )
    }

    /**
     * Deletes one object from the configured bucket.
     *
     * @param objectKey Storage object key persisted on the attachment entity.
     */
    override fun delete(objectKey: String) {
        try {
            s3Client.deleteObject(
                DeleteObjectRequest.builder()
                    .bucket(properties.bucket)
                    .key(objectKey)
                    .build()
            )
        } catch (ex: SdkException) {
            throw FileStorageException("Could not delete stored object '$objectKey'.", ex)
        }
    }

    /**
     * Loads one object from the configured bucket for download or SMTP delivery.
     *
     * @param objectKey Storage object key persisted on the attachment entity.
     * @return Binary object data with content type and length metadata.
     */
    override fun load(objectKey: String): StoredAttachmentObject {
        val responseBytes = try {
            s3Client.getObjectAsBytes(
                GetObjectRequest.builder()
                    .bucket(properties.bucket)
                    .key(objectKey)
                    .build()
            )
        } catch (ex: S3Exception) {
            throw mapLoadFailure(objectKey, ex)
        } catch (ex: SdkException) {
            throw FileStorageException("Stored object '$objectKey' is not readable.", ex)
        }

        return responseBytes.toStoredObject()
    }

    /**
     * Converts an S3 byte response into the storage-independent attachment object.
     */
    private fun ResponseBytes<GetObjectResponse>.toStoredObject(): StoredAttachmentObject {
        return StoredAttachmentObject(
            resource = ByteArrayResource(asByteArray()),
            contentType = response().contentType(),
            contentLength = response().contentLength()
        )
    }

    /**
     * Maps S3 load errors to application storage exceptions with safe messages.
     */
    private fun mapLoadFailure(objectKey: String, ex: S3Exception): RuntimeException {
        return if (ex.statusCode() == HTTP_NOT_FOUND) {
            FileStorageObjectNotFoundException("Stored object '$objectKey' was not found.", ex)
        } else {
            FileStorageException("Stored object '$objectKey' is not readable.", ex)
        }
    }

    /**
     * Removes path information from client-provided filenames before they become metadata.
     */
    private fun sanitizeFilename(originalFilename: String?): String {
        return originalFilename
            ?.takeIf { name -> name.isNotBlank() }
            ?.let { name -> Paths.get(name).fileName.toString() }
            ?: DEFAULT_ATTACHMENT_FILENAME
    }

    /**
     * Keeps the original extension in generated object keys so downloads remain recognizable.
     */
    private fun fileExtension(filename: String): String {
        val extensionStart = filename.lastIndexOf(".")
        return if (extensionStart > 0 && extensionStart < filename.lastIndex) {
            filename.substring(extensionStart)
        } else {
            ""
        }
    }

    private companion object {
        const val DEFAULT_ATTACHMENT_FILENAME = "attachment"
        const val ORIGINAL_FILENAME_METADATA = "original-filename"
        const val HTTP_NOT_FOUND = 404
    }
}
