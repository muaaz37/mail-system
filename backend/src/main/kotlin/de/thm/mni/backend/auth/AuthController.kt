package de.thm.mni.backend.auth

import de.thm.mni.backend.auth.dto.LoginRequest
import de.thm.mni.backend.auth.dto.AuthResponse
import de.thm.mni.backend.auth.dto.RegisterRequest
import de.thm.mni.backend.error.InvalidCredentialsException
import de.thm.mni.backend.error.ResourceAlreadyExistsException
import de.thm.mni.backend.security.JwtService
import de.thm.mni.backend.user.User
import de.thm.mni.backend.user.UserService
import de.thm.mni.backend.user.dto.toDTO
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController


@RestController
class AuthController(
    private val userService: UserService,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService
) {

    @PostMapping("/api/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@Valid @RequestBody data: RegisterRequest): AuthResponse {
        val existingUser = userService.existsUserByEmail(data.email)

        if (existingUser) {
            throw ResourceAlreadyExistsException("Email is already in use by another user.")
        }
        val encodedPassword = passwordEncoder.encode(data.password).toString()

        val user = User(
            firstName = data.firstName,
            lastName = data.lastName,
            email = data.email,
            password = encodedPassword
        )
        val createdUser = userService.createUser(user)

        val token = jwtService.createToken(createdUser.id.toString())

        return AuthResponse(createdUser.toDTO(), token)

    }

    @PostMapping("/api/login")
    fun login(@Valid @RequestBody data: LoginRequest): AuthResponse {
        val user = userService.getUserByEmail(data.email)
            ?: throw InvalidCredentialsException("Invalid credentials")

        val isPasswordValid = passwordEncoder.matches(data.password, user.password)
        if (!isPasswordValid) {
            throw InvalidCredentialsException("Invalid credentials")
        }

        val token = jwtService.createToken(user.id.toString())

        return AuthResponse(user.toDTO(), token)
    }
}