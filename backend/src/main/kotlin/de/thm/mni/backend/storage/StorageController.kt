package de.thm.mni.backend.storage

import de.thm.mni.backend.openapi.BearerAuthenticated
import de.thm.mni.backend.openapi.DefaultApiErrors
import de.thm.mni.backend.openapi.BadGatewayApiResponse
import de.thm.mni.backend.openapi.NotFoundApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
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
@BearerAuthenticated
@DefaultApiErrors
@RestController
@RequestMapping("/api/images")
class StorageController(private val fileStorageService: FileStorageService) {
    /**
     * Loads a stored resource by object key and returns it with stored media metadata.
     */
    @GetMapping("/{objectKey}")
    @Operation(operationId = "downloadAttachment", summary = "Download an attachment", description = "Returns stored attachment content with its original media type and length metadata.")
    @ApiResponse(responseCode = "200", description = "Attachment returned successfully.")
    @NotFoundApiResponse
    @BadGatewayApiResponse
    fun getImage(
        @Parameter(description = "Storage object key from an attachment's `path` property.")
        @PathVariable objectKey: String
    ): ResponseEntity<Resource> {
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
