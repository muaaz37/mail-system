package de.thm.mni.backend.imap

import de.thm.mni.backend.imap.dto.ImapMailPreview
import de.thm.mni.backend.mail.Mail
import de.thm.mni.backend.mail.MailImportService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Exposes authenticated IMAP diagnostics and a manual import trigger.
 */
@Tag(name = "IMAP Diagnostics", description = "Inspect and trigger support mailbox imports.")
@RestController
@RequestMapping("/api/imap/diagnostics")
class ImapDiagnosticsController(
    private val imapService: IMAPService,
    private val mailImportService: MailImportService
) {
    /**
     * Returns the number of unread messages currently visible through IMAP.
     */
    @GetMapping("/unread-count")
    fun unreadCount(): Int {
        return imapService.countUnreadMessages()
    }

    /**
     * Returns short unread message previews for troubleshooting imports.
     */
    @GetMapping("/unread-previews")
    fun unreadPreviews(): List<ImapMailPreview> {
        return imapService.getUnreadMailData()
    }

    /**
     * Starts an immediate import run for unread messages.
     */
    @GetMapping("/import")
    fun importMails(): List<Mail> {
        return mailImportService.importUnreadMails()
    }
}
