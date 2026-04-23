package de.thm.mni.backend.storage

import de.thm.mni.backend.attachment.dto.AttachmentDTO
import jakarta.annotation.PostConstruct
import org.springframework.core.io.Resource
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.UrlResource
import org.springframework.stereotype.Repository
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.net.MalformedURLException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.UUID


@Repository
class FileStorageRepository(@Value("\${file.upload-dir}") private val uploadDir: String) {

    private var rootLocation: Path? = null

    @PostConstruct
    fun init() {
        try {
            this.rootLocation = Paths.get(uploadDir)
            Files.createDirectories(rootLocation)
        } catch (e: IOException) {
            throw RuntimeException("Could not initialize folder for upload!")
        }
    }

    fun saveFile(file: MultipartFile): AttachmentDTO? {
        try {
            if (file.isEmpty) {
                return null
            }

            val originalFilename = file.originalFilename

            val extension = originalFilename!!.substring(originalFilename.lastIndexOf("."))
            val newFilename = UUID.randomUUID().toString() + extension

            val destinationFile: Path? = this.rootLocation?.resolve(Paths.get(newFilename))
                ?.normalize()?.toAbsolutePath()

            Files.copy(file.inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING)

            return AttachmentDTO(
                size = file.size,
                fileName = file.originalFilename,
                mimeType = file.contentType,
                path = newFilename
            )
        } catch (e: IOException) {
            throw RuntimeException("Failed to store file.", e)
        }
    }

    fun deleteFile(filename: String) {
        val filePath = rootLocation?.resolve(filename)
            ?: throw RuntimeException("Could not delete the file!")

        try {
            Files.deleteIfExists(filePath)
        } catch (e: IOException) {
            throw RuntimeException("Could not delete the file!", e)
        }
    }

    fun load(filename: String): Resource {
        try {
            val file = rootLocation?.resolve(filename) ?: throw RuntimeException("File not found!")
            val resource = UrlResource(file.toUri())

            if (resource.exists() || resource.isReadable) {
                return resource
            } else {
                throw RuntimeException("Could not read the file!")
            }
        } catch (e: MalformedURLException) {
            throw RuntimeException("Error: " + e.message)
        }
    }
}