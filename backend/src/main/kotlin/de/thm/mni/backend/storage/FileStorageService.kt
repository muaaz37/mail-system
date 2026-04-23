package de.thm.mni.backend.storage

import de.thm.mni.backend.attachment.dto.AttachmentDTO
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile


@Service
class FileStorageService(private val fileStorageRepository: FileStorageRepository) {

    fun saveFile(file: MultipartFile): AttachmentDTO? {
        return fileStorageRepository.saveFile(file)
    }

    fun deleteFile(filename: String?) {
        if(filename == null){
            throw RuntimeException("Filename is null")
        }
        return fileStorageRepository.deleteFile(filename)
    }

    fun load(filename: String): Resource {
        return fileStorageRepository.load(filename)
    }
}