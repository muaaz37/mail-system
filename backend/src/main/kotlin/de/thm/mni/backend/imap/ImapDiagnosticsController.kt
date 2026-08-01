package de.thm.mni.backend.imap

import de.thm.mni.backend.imap.dto.ImapMailPreview
import de.thm.mni.backend.mail.MailAccessService
import de.thm.mni.backend.mail.MailImportService
import de.thm.mni.backend.mail.MailMapper
import de.thm.mni.backend.mail.dto.MailDTO
import de.thm.mni.backend.openapi.BearerAuthenticated
import de.thm.mni.backend.openapi.DefaultApiErrors
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Exposes authenticated IMAP diagnostics and a manual import trigger.
 */
@Tag(name = "IMAP Diagnostics", description = "Inspect and trigger support mailbox imports.")
@BearerAuthenticated
@DefaultApiErrors
@RestController
@RequestMapping("/api/imap/diagnostics", produces = [MediaType.APPLICATION_JSON_VALUE])
class ImapDiagnosticsController(
    private val imapService: IMAPService,
    private val mailImportService: MailImportService,
    private val mailMapper: MailMapper,
    private val mailAccessService: MailAccessService
) {
    /**
     * Returns the number of unread messages currently visible through IMAP.
     */
    @GetMapping("/unread-count")
    @Operation(operationId = "getUnreadImapCount", summary = "Count unread mailbox messages", description = "Returns the number of unread messages in the configured support mailbox.")
    @ApiResponse(responseCode = "200", description = "Unread message count returned successfully.")
    fun unreadCount(): Int {
        return imapService.countUnreadMessages()
    }

    /**
     * Returns short unread message previews for troubleshooting imports.
     */
    @GetMapping("/unread-previews")
    @Operation(operationId = "getUnreadImapPreviews", summary = "Preview unread mailbox messages", description = "Returns lightweight previews of unread support-mailbox messages for diagnostics.")
    @ApiResponse(responseCode = "200", description = "Unread message previews returned successfully.")
    fun unreadPreviews(): List<ImapMailPreview> {
        return imapService.getUnreadMailData()
    }

    /**
     * Starts an immediate import run for unread messages.
     */
    @PostMapping("/import")
    @Operation(operationId = "importUnreadImapMails", summary = "Import unread mailbox messages", description = "Triggers an immediate import from the configured support mailbox.")
    @ApiResponse(responseCode = "200", description = "Import completed and imported mails returned successfully.")
    fun importMails(@AuthenticationPrincipal jwt: Jwt): List<MailDTO> {
        val user = mailAccessService.authenticatedUser(jwt)
        return mailImportService.importUnreadMails().map { mail -> mailMapper.toDTO(user, mail) }
    }
}
