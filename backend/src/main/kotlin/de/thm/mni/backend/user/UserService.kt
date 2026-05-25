package de.thm.mni.backend.user

import org.springframework.stereotype.Service
import java.util.UUID


@Service
class UserService(private val userRepository: UserRepository){

    fun createUser(user: User): User {
        return userRepository.save(user)
    }

    fun getUserById(id: UUID): User? {
        return userRepository.findById(id).orElse(null)
    }

    fun existsUserByEmail(email: String): Boolean {
        return userRepository.existsUserByEmail(email)
    }

    fun getUserByEmail(email: String): User? {
        return userRepository.findUserByEmail(email)
    }

    fun findByEmail(email: String): User? {
        return userRepository.findByEmail(email)
    }

    fun getAllUsers(): List<User> {
        return userRepository.findAll().toList()
    }

    fun createExternalUser(email: String): User {
        val existingUser = userRepository.findByEmail(email)
        if (existingUser != null) {
            return existingUser
        }

        val externalUser = User(
            firstName = "External",
            lastName = "Sender",
            email = email,
            password = ""
        )

        return userRepository.save(externalUser)
    }

    fun updateUser(id: UUID, updatedUser: User): User {
        return userRepository.save(updatedUser)
    }

    fun deleteUser(id: UUID) {
        userRepository.deleteById(id)
    }
}