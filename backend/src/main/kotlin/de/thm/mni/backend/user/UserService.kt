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
     * Checks whether an email address is already registered.
     */
    fun existsUserByEmail(email: String): Boolean {
        return userRepository.existsUserByEmail(email)
    }

    /**
     * Loads a user by email address.
     */
    fun getUserByEmail(email: String): User? {
        return userRepository.findUserByEmail(email)
    }

    /**
     * Returns all registered users.
     */
    fun getAllUsers(): List<User> {
        return userRepository.findAll().toList()
    }

    /**
     * Replaces stored user profile data for the given user identifier.
     */
    fun updateUser(id: UUID, updatedUser: User): User {
        updatedUser.id = id
        return userRepository.save(updatedUser)
    }

    /**
     * Deletes a user account by identifier.
     */
    fun deleteUser(id: UUID) {
        userRepository.deleteById(id)
    }

    /**
     * Loads the local profile linked to an OpenID Connect subject.
     */
    fun getUserByIdentitySubject(identitySubject: String): User? {
        return userRepository.findByIdentitySubject(identitySubject)
    }
}
