package de.thm.mni.backend.attachment.dto

import de.thm.mni.backend.attachment.Attachment

/**
 * API model for attachment metadata returned to clients.
 */
data class AttachmentDTO(
    val size: Long,
    val fileName: String?,
    val mimeType: String?,
    val path: String,
)

/**
 * Converts a stored attachment entity to its response DTO.
 */
fun Attachment.toDTO() = AttachmentDTO(
    fileName = this.fileName,
    size = this.size,
    mimeType = this.mimeType,
    path = this.path,
)
