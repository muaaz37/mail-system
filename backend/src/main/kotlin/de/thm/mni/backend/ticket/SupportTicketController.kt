package de.thm.mni.backend.ticket

import de.thm.mni.backend.mail.MailMapper
import de.thm.mni.backend.mail.MailAccessService
import de.thm.mni.backend.ticket.dto.SupportTicketDTO
import de.thm.mni.backend.ticket.dto.SupportTicketDetailDTO
import de.thm.mni.backend.ticket.dto.UpdateTicketPriorityRequest
import de.thm.mni.backend.openapi.BearerAuthenticated
import de.thm.mni.backend.openapi.DefaultApiErrors
import de.thm.mni.backend.openapi.BadRequestApiResponse
import de.thm.mni.backend.openapi.NotFoundApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * Provides the HTTP API for support ticket workflow and shared support views.
 */
@Tag(name = "Support Ticket", description = "Manage shared support tickets and lifecycle states.")
@BearerAuthenticated
@DefaultApiErrors
@RestController
@RequestMapping("/api/tickets", produces = [MediaType.APPLICATION_JSON_VALUE])
class SupportTicketController(
    private val ticketLifecycleService: SupportTicketLifecycleService,
    private val ticketCommandService: SupportTicketCommandService,
    private val ticketReadStateService: SupportTicketReadStateService,
    private val ticketMapper: SupportTicketMapper,
    private val mailMapper: MailMapper,
    private val mailAccessService: MailAccessService
) {
    /**
     * Lists shared support tickets. Supported views: open, waiting, resolved, all.
     */
    @GetMapping
    @Operation(operationId = "getSupportTickets", summary = "List support tickets", description = "Returns support tickets filtered by the requested workflow view. Supported values are `open`, `waiting`, `resolved`, and `all`; unknown values use the `open` view.")
    @ApiResponse(responseCode = "200", description = "Support tickets returned successfully.")
    fun getTickets(
        @Parameter(description = "Workflow view used to filter tickets.", example = "open") @RequestParam(defaultValue = "open") view: String,
        @AuthenticationPrincipal jwt: Jwt
    ): List<SupportTicketDTO> {
        val user = mailAccessService.authenticatedUser(jwt)
        return ticketLifecycleService.listTickets(view).map { ticket -> ticketMapper.toDTO(ticket, user) }
    }

    /**
     * Returns a ticket with its complete mail conversation.
     */
    @GetMapping("/{ticketId}")
    @Operation(operationId = "getSupportTicket", summary = "Get a support ticket", description = "Returns ticket metadata and its complete mail conversation and marks it as read.")
    @ApiResponse(responseCode = "200", description = "Support ticket returned successfully.")
    @NotFoundApiResponse
    fun getTicket(
        @PathVariable ticketId: UUID,
        @AuthenticationPrincipal jwt: Jwt
    ): SupportTicketDetailDTO {
        val user = mailAccessService.authenticatedUser(jwt)
        val ticket = ticketCommandService.ticketOrNotFound(ticketId)
        val mails = ticket.mails
            .sortedBy { mail -> mail.createdAt }
            .map { mail -> mailMapper.toDTO(user, mail) }
        ticketReadStateService.markRead(ticket, user)

        return SupportTicketDetailDTO(ticket = ticketMapper.toDTO(ticket, user), mails = mails)
    }

    /**
     * Assigns the ticket to the current user.
     */
    @PostMapping("/{ticketId}/assign/me")
    @Operation(operationId = "assignSupportTicketToMe", summary = "Assign a ticket to me", description = "Assigns the support ticket to the authenticated user.")
    @ApiResponse(responseCode = "200", description = "Support ticket assigned successfully.")
    @NotFoundApiResponse
    fun assignToMe(
        @PathVariable ticketId: UUID,
        @AuthenticationPrincipal jwt: Jwt
    ): SupportTicketDTO {
        val user = mailAccessService.authenticatedUser(jwt)
        val ticket = ticketCommandService.assignTo(ticketId, user)
        ticketReadStateService.markRead(ticket, user)
        return ticketMapper.toDTO(ticket, user)
    }

    /**
     * Removes the current assignee from the ticket.
     */
    @DeleteMapping("/{ticketId}/assign")
    @Operation(operationId = "unassignSupportTicket", summary = "Unassign a ticket", description = "Removes the current assignee from the support ticket.")
    @ApiResponse(responseCode = "200", description = "Support ticket unassigned successfully.")
    @NotFoundApiResponse
    fun unassign(
        @PathVariable ticketId: UUID,
        @AuthenticationPrincipal jwt: Jwt
    ): SupportTicketDTO {
        val user = mailAccessService.authenticatedUser(jwt)
        val ticket = ticketCommandService.unassign(ticketId)
        ticketReadStateService.markRead(ticket, user)
        return ticketMapper.toDTO(ticket, user)
    }

    /**
     * Marks a ticket as resolved.
     */
    @PostMapping("/{ticketId}/resolve")
    @Operation(operationId = "resolveSupportTicket", summary = "Resolve a ticket", description = "Marks the support ticket as resolved.")
    @ApiResponse(responseCode = "200", description = "Support ticket resolved successfully.")
    @NotFoundApiResponse
    fun resolve(
        @PathVariable ticketId: UUID,
        @AuthenticationPrincipal jwt: Jwt
    ): SupportTicketDTO {
        val user = mailAccessService.authenticatedUser(jwt)
        val ticket = ticketCommandService.resolveTicket(ticketId)
        ticketReadStateService.markRead(ticket, user)
        return ticketMapper.toDTO(ticket, user)
    }

    /**
     * Reopens a resolved ticket manually.
     */
    @PostMapping("/{ticketId}/reopen")
    @Operation(operationId = "reopenSupportTicket", summary = "Reopen a ticket", description = "Moves a resolved support ticket back into the active workflow.")
    @ApiResponse(responseCode = "200", description = "Support ticket reopened successfully.")
    @NotFoundApiResponse
    fun reopen(
        @PathVariable ticketId: UUID,
        @AuthenticationPrincipal jwt: Jwt
    ): SupportTicketDTO {
        val user = mailAccessService.authenticatedUser(jwt)
        val ticket = ticketCommandService.reopenTicket(ticketId)
        ticketReadStateService.markRead(ticket, user)
        return ticketMapper.toDTO(ticket, user)
    }

    /**
     * Updates ticket priority.
     */
    @PutMapping("/{ticketId}/priority")
    @Operation(operationId = "updateSupportTicketPriority", summary = "Update ticket priority", description = "Changes the workflow priority of a support ticket.")
    @ApiResponse(responseCode = "200", description = "Support ticket priority updated successfully.")
    @BadRequestApiResponse
    @NotFoundApiResponse
    fun updatePriority(
        @PathVariable ticketId: UUID,
        @RequestBody request: UpdateTicketPriorityRequest,
        @AuthenticationPrincipal jwt: Jwt
    ): SupportTicketDTO {
        val user = mailAccessService.authenticatedUser(jwt)
        val ticket = ticketCommandService.updatePriority(ticketId, request.priority)
        ticketReadStateService.markRead(ticket, user)
        return ticketMapper.toDTO(ticket, user)
    }
}
