package de.thm.mni.backend.health

import de.thm.mni.backend.health.dto.HealthResponse
import io.swagger.v3.oas.annotations.Hidden
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

/**
 * Provides application liveness information for container health checks.
 */
@Hidden
@RestController
@RequestMapping("/api/health")
class HealthController(
    @Value("\${spring.application.name}")
    private val applicationName: String
) {

    /**
     * Reports whether the application is running and able to process HTTP requests.
     */
    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getHealth(): HealthResponse =
        HealthResponse(
            status = "UP",
            service = applicationName,
            timestamp = Instant.now()
        )
}
