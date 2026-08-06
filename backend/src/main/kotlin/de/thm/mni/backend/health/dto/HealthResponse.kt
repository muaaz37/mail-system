package de.thm.mni.backend.health.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

/**
 * Public response returned by the application health endpoint.
 */
@Schema(description = "Current health status of the mail server.")
data class HealthResponse(

    @field:Schema(
        description = "Current application health state.",
        example = "UP"
    )
    val status: String,

    @field:Schema(
        description = "Name of the running application.",
        example = "backend"
    )
    val service: String,

    @field:Schema(
        description = "Time at which the health status was generated.",
        example = "2026-08-04T14:30:00Z"
    )
    val timestamp: Instant
)
