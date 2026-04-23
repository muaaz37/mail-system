package de.thm.mni.backend.util

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder


class SaltPepperPasswordEncoder(val pepper: String) : PasswordEncoder {
  init { require(pepper.isNotBlank()) { "Pepper must not be blank" } }

  private val encoder = BCryptPasswordEncoder()

  private fun extendWithPepper(rawPassword: CharSequence?): String =
    (rawPassword?.toString() ?: "") + pepper

  override fun encode(rawPassword: CharSequence?): String? {
     return  encoder.encode(extendWithPepper(rawPassword))
  }

  override fun matches(rawPassword: CharSequence?, encodedPassword: String?): Boolean =
    encoder.matches(extendWithPepper(rawPassword), encodedPassword)

}
