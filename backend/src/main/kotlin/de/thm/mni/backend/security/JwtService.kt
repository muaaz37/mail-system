package de.thm.mni.backend.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import java.util.Date


@Service
class JwtService(
    @Value("\${app.jwt.secret}") private val secret: String,
    @Value("\${app.name}") private val appName: String,
    @Value("\${app.jwt.expires-seconds}") private val expiresSeconds: Long,
)
{

    fun createToken(subject: String): String {
        val now = Instant.now()
        val exp = now.plusSeconds(expiresSeconds)

        return JWT.create()
            .withIssuer(appName)
            .withSubject(subject)
            .withIssuedAt(Date.from(now))
            .withExpiresAt(Date.from(exp))
            .sign(Algorithm.HMAC256(secret))

    }

    private val verifier = JWT
        .require(Algorithm.HMAC256(secret))
        .withIssuer(appName)
        .build()

    fun validate(token: String): DecodedJWT = verifier.verify(token)
}