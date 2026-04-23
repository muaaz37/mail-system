package de.thm.mni.backend.attachment.dto

import de.thm.mni.backend.attachment.Attachment

data class AttachmentDTO(
    val size: Long,
    val fileName: String?,
    val mimeType: String?,
    val path: String,
)

fun Attachment.toDTO() = AttachmentDTO(
    fileName = this.fileName,
    size = this.size,
    mimeType = this.mimeType,
    path = this.path,
)