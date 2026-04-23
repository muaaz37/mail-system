package de.thm.mni.backend.storage

import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.nio.file.Files
import java.nio.file.Paths


@RestController
@RequestMapping("/api/images")
class StorageController(private val fileStorageService: FileStorageService) {
    @GetMapping("/{filename}")
    fun getImage(@PathVariable filename: String): ResponseEntity<Resource> {
        val file: Resource = fileStorageService.load(filename)

        val contentType =  MediaType.parseMediaType(Files.probeContentType(Paths.get(file.uri)))

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType.toString()))
            .body(file)
    }
}