package de.thm.mni.backend.storage

/** Detects the small set of attachment formats that may be rendered inline. */
@Suppress("MagicNumber")
object AttachmentContentTypes {
    const val BINARY = "application/octet-stream"
    const val PDF = "application/pdf"
    const val PNG = "image/png"
    const val JPEG = "image/jpeg"
    const val GIF = "image/gif"
    const val WEBP = "image/webp"

    // The set of attachment formats that may be rendered inline.
    private val previewableTypes = setOf(PDF, PNG, JPEG, GIF, WEBP)

    /**
     * Detects the attachment format based on its first few bytes.
     *
     * @param bytes The attachment bytes.
     * @return The detected format, or `null` if the format is not supported.
     */
    fun detect(bytes: ByteArray): String? = when {
        bytes.startsWith(0x25, 0x50, 0x44, 0x46, 0x2D) -> PDF
        bytes.startsWith(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) -> PNG
        bytes.startsWith(0xFF, 0xD8, 0xFF) -> JPEG
        bytes.startsWithAscii("GIF87a") || bytes.startsWithAscii("GIF89a") -> GIF
        bytes.size >= 12 && bytes.startsWithAscii("RIFF") && bytes.matchesAscii(8, "WEBP") -> WEBP
        else -> null
    }

    /** Normalizes the attachment content type to a format that may be rendered inline. */
    fun normalizeClaimedType(contentType: String?): String? = when (contentType?.lowercase()?.trim()) {
        "image/jpg" -> JPEG
        in previewableTypes -> contentType?.lowercase()?.trim()
        else -> null
    }

    /** Returns a safe response content type for the given attachment. */
    fun safeResponseType(contentType: String?): String {
        return normalizeClaimedType(contentType) ?: BINARY
    }

    /** Returns whether the given attachment content type may be rendered inline. */
    fun isPreviewable(contentType: String): Boolean = contentType in previewableTypes

    /** Returns whether the given byte array starts with the specified signature. */
    private fun ByteArray.startsWith(vararg signature: Int): Boolean {
        return size >= signature.size && signature.indices.all { index ->
            this[index].toInt() and 0xFF == signature[index]
        }
    }

    /** Returns whether the given byte array starts with the specified ASCII string. */
    private fun ByteArray.startsWithAscii(value: String): Boolean = matchesAscii(0, value)

    /** Returns whether the given byte array matches the specified ASCII string. */
    private fun ByteArray.matchesAscii(offset: Int, value: String): Boolean {
        if (size < offset + value.length) return false
        return value.indices.all { index -> this[offset + index].toInt() == value[index].code }
    }
}
