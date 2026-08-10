package de.thm.mni.backend.user

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID


/**
 * Provides database access for local user profiles.
 */
@Repository
interface UserRepository : CrudRepository<User, UUID> {
    fun existsUserByEmail(email: String): Boolean
    fun findUserByEmail(email: String): User?
    fun findByIdentitySubject(identitySubject: String): User?
}
