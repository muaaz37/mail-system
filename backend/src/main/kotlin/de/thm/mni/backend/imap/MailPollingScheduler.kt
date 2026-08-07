package de.thm.mni.backend.imap

import de.thm.mni.backend.config.MailPollingProperties
import de.thm.mni.backend.mail.external.MailImportService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Runs the periodic IMAP import for unread support mailbox messages.
 */
@Component
class MailPollingScheduler(
    private val mailImportService: MailImportService,
    private val mailPollingProperties: MailPollingProperties
) {
    private val logger = LoggerFactory.getLogger(MailPollingScheduler::class.java)

    /**
     * Executes one polling cycle and keeps future cycles alive when an import fails.
     */
    @Suppress("TooGenericExceptionCaught")
    @Scheduled(fixedDelayString = "\${mail.polling.interval-ms}")
    fun importUnreadMails() {
        try {
            val importedMails = mailImportService.importUnreadMails()
            if (importedMails.isNotEmpty()) {
                logger.info(
                    "Imported {} unread IMAP mails. Next poll in {} ms.",
                    importedMails.size,
                    mailPollingProperties.intervalMs
                )
            }
        } catch (ex: Exception) {
            logger.error("Scheduled IMAP import failed. The next polling cycle will retry.", ex)
        }
    }
}
