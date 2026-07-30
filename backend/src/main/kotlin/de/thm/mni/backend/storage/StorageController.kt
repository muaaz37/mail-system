package de.thm.mni.backend.storage

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


/**
 * Provides authenticated access to stored attachment resources.
 */
@Tag(name = "Attachment", description = "Download stored mail attachments.")
@RestController
@RequestMapping("/api/images")
class StorageController(private val fileStorageService: FileStorageService) {
    /**
     * Loads a stored resource by object key and returns it with stored media metadata.
     */
    @GetMapping("/{objectKey}")
    fun getImage(@PathVariable objectKey: String): ResponseEntity<Resource> {
        val storedObject = fileStorageService.load(objectKey)
        val contentType = storedObject.contentType
            ?.takeIf { value -> value.isNotBlank() }
            ?.let { value -> MediaType.parseMediaType(value) }
            ?: MediaType.APPLICATION_OCTET_STREAM

        val response = ResponseEntity.ok().contentType(contentType)
        val contentLength = storedObject.contentLength

        return if (contentLength != null) {
            response.contentLength(contentLength).body(storedObject.resource)
        } else {
            response.body(storedObject.resource)
        }
    }
}
