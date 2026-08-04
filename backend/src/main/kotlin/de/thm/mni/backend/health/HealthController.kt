package de.thm.mni.backend.health

import de.thm.mni.backend.health.dto.HealthResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

/**
 * Provides application liveness information for container health checks.
 */
@Tag(name = "Health", description = "Application health monitoring.")
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
    @Operation(
        operationId = "getApplicationHealth",
        summary = "Get application health",
        description = "Returns the current liveness status of the backend application."
    )
    @ApiResponse(
        responseCode = "200",
        description = "The application is running."
    )
    fun getHealth(): HealthResponse =
        HealthResponse(
            status = "UP",
            service = applicationName,
            timestamp = Instant.now()
        )
}