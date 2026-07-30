package de.thm.mni.backend.util

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder


/**
 * Password encoder that appends an application-wide pepper before BCrypt hashing.
 */
class SaltPepperPasswordEncoder(val pepper: String) : PasswordEncoder {
  init { require(pepper.isNotBlank()) { "Pepper must not be blank" } }

  private val encoder = BCryptPasswordEncoder()

  /**
   * Adds the configured pepper to the raw password before hashing or verification.
   */
  private fun extendWithPepper(rawPassword: CharSequence?): String =
    (rawPassword?.toString() ?: "") + pepper

  /**
   * Hashes a raw password with BCrypt after adding the pepper.
   */
  override fun encode(rawPassword: CharSequence?): String? {
     return  encoder.encode(extendWithPepper(rawPassword))
  }

  /**
   * Verifies a raw password against a stored BCrypt hash after adding the pepper.
   */
  override fun matches(rawPassword: CharSequence?, encodedPassword: String?): Boolean =
    encoder.matches(extendWithPepper(rawPassword), encodedPassword)

}
