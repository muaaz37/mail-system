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


    fun getAllUsers(): List<User> {
        return userRepository.findAll().toList()
    }

    fun updateUser(id: UUID, updatedUser: User): User {
        return userRepository.save(updatedUser)
    }

    fun deleteUser(id: UUID) {
        userRepository.deleteById(id)
    }

}