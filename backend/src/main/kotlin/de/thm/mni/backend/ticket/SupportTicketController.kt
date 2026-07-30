package de.thm.mni.backend.ticket

import de.thm.mni.backend.mail.MailMapper
import de.thm.mni.backend.mail.MailAccessService
import de.thm.mni.backend.ticket.dto.SupportTicketDTO
import de.thm.mni.backend.ticket.dto.SupportTicketDetailDTO
import de.thm.mni.backend.ticket.dto.UpdateTicketPriorityRequest
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
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
@RestController
@RequestMapping("/api/tickets")
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
    fun getTickets(
        @RequestParam(defaultValue = "open") view: String,
        @AuthenticationPrincipal userDetails: UserDetails
    ): List<SupportTicketDTO> {
        val user = mailAccessService.authenticatedUser(userDetails)
        return ticketLifecycleService.listTickets(view).map { ticket -> ticketMapper.toDTO(ticket, user) }
    }

    /**
     * Returns a ticket with its complete mail conversation.
     */
    @GetMapping("/{ticketId}")
    fun getTicket(
        @PathVariable ticketId: UUID,
        @AuthenticationPrincipal userDetails: UserDetails
    ): SupportTicketDetailDTO {
        val user = mailAccessService.authenticatedUser(userDetails)
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
    fun assignToMe(
        @PathVariable ticketId: UUID,
        @AuthenticationPrincipal userDetails: UserDetails
    ): SupportTicketDTO {
        val user = mailAccessService.authenticatedUser(userDetails)
        val ticket = ticketCommandService.assignTo(ticketId, user)
        ticketReadStateService.markRead(ticket, user)
        return ticketMapper.toDTO(ticket, user)
    }

    /**
     * Removes the current assignee from the ticket.
     */
    @DeleteMapping("/{ticketId}/assign")
    fun unassign(
        @PathVariable ticketId: UUID,
        @AuthenticationPrincipal userDetails: UserDetails
    ): SupportTicketDTO {
        val user = mailAccessService.authenticatedUser(userDetails)
        val ticket = ticketCommandService.unassign(ticketId)
        ticketReadStateService.markRead(ticket, user)
        return ticketMapper.toDTO(ticket, user)
    }

    /**
     * Marks a ticket as resolved.
     */
    @PostMapping("/{ticketId}/resolve")
    fun resolve(
        @PathVariable ticketId: UUID,
        @AuthenticationPrincipal userDetails: UserDetails
    ): SupportTicketDTO {
        val user = mailAccessService.authenticatedUser(userDetails)
        val ticket = ticketCommandService.resolveTicket(ticketId)
        ticketReadStateService.markRead(ticket, user)
        return ticketMapper.toDTO(ticket, user)
    }

    /**
     * Reopens a resolved ticket manually.
     */
    @PostMapping("/{ticketId}/reopen")
    fun reopen(
        @PathVariable ticketId: UUID,
        @AuthenticationPrincipal userDetails: UserDetails
    ): SupportTicketDTO {
        val user = mailAccessService.authenticatedUser(userDetails)
        val ticket = ticketCommandService.reopenTicket(ticketId)
        ticketReadStateService.markRead(ticket, user)
        return ticketMapper.toDTO(ticket, user)
    }

    /**
     * Updates ticket priority.
     */
    @PutMapping("/{ticketId}/priority")
    fun updatePriority(
        @PathVariable ticketId: UUID,
        @RequestBody request: UpdateTicketPriorityRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): SupportTicketDTO {
        val user = mailAccessService.authenticatedUser(userDetails)
        val ticket = ticketCommandService.updatePriority(ticketId, request.priority)
        ticketReadStateService.markRead(ticket, user)
        return ticketMapper.toDTO(ticket, user)
    }
}
