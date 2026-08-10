package de.thm.mni.backend.storage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AttachmentContentTypesTests {
    @Test
    fun `detects supported attachment signatures`() {
        assertEquals(AttachmentContentTypes.PDF, AttachmentContentTypes.detect("%PDF-1.7".encodeToByteArray()))
        assertEquals(
            AttachmentContentTypes.PNG,
            AttachmentContentTypes.detect(
                byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
            )
        )
        assertEquals(
            AttachmentContentTypes.JPEG,
            AttachmentContentTypes.detect(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()))
        )
    }

    @Test
    fun `rejects active or unknown file content`() {
        assertNull(AttachmentContentTypes.detect("<script>alert(1)</script>".encodeToByteArray()))
        assertNull(AttachmentContentTypes.detect("plain text".encodeToByteArray()))
    }

    @Test
    fun `serves unknown metadata as binary content`() {
        assertEquals(
            AttachmentContentTypes.BINARY,
            AttachmentContentTypes.safeResponseType("text/html")
        )
    }
}
