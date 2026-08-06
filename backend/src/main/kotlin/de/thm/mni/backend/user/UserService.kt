package de.thm.mni.backend.user

import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Encapsulates user persistence operations used by controllers and mail services.
 */
@Service
class UserService(private val userRepository: UserRepository) {
    /**
     * Stores a new user account.
     */
    fun createUser(user: User): User {
        return userRepository.save(user)
    }

    /**
     * Loads a user by identifier.
     */
    fun getUserById(id: UUID): User? {
        return userRepository.findById(id).orElse(null)
    }

    /**
     * Loads a user by email address.
     */
    fun getUserByEmail(email: String): User? {
        return userRepository.findUserByEmail(email)
    }

    /**
     * Returns all local profiles synchronized from Keycloak.
     */
    fun getAllUsers(): List<User> {
        return userRepository.findAll().toList()
    }

    /**
     * Loads the local profile linked to an OpenID Connect subject.
     */
    fun getUserByIdentitySubject(identitySubject: String): User? {
        return userRepository.findByIdentitySubject(identitySubject)
    }
}
