package de.thm.mni.backend.attachment

import org.springframework.data.repository.CrudRepository
import java.util.UUID

/** Provides attachment metadata required for authorization checks. */
interface AttachmentRepository : CrudRepository<Attachment, UUID> {
    fun findByPath(path: String): Attachment?
}
