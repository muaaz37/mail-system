package de.thm.mni.backend.ticket

import de.thm.mni.backend.mail.MailRepository
import de.thm.mni.backend.mail.enums.MailDeliveryMode
import de.thm.mni.backend.mail.enums.MailStatus
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

/**
 * Creates ticket records for already imported support mails when upgrading older databases.
 */
@Component
class SupportTicketBackfillRunner(
    private val mailRepository: MailRepository,
    private val ticketLifecycleService: SupportTicketLifecycleService
) : ApplicationRunner {
    private val logger = LoggerFactory.getLogger(SupportTicketBackfillRunner::class.java)

    override fun run(args: ApplicationArguments) {
        // Tickets represent external support conversations only. Internal mails are
        // displayed separately by the client and must never be backfilled as tickets.
        val supportMailsWithoutTicket = mailRepository.findAllByStatus(MailStatus.RECEIVED)
            .filter { mail -> mail.deliveryMode == MailDeliveryMode.EXTERNAL && mail.ticket == null }

        supportMailsWithoutTicket.forEach { mail ->
            ticketLifecycleService.attachIncomingMail(mail)
            mailRepository.save(mail)
        }

        val backfilledTicketCount = supportMailsWithoutTicket.size
        if (backfilledTicketCount > 0) {
            logger.info("Backfilled {} tickets for existing mails.", backfilledTicketCount)
        }
    }
}
