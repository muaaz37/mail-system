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
        val supportMailsWithoutTicket = mailRepository.findAllByStatus(MailStatus.RECEIVED)
            .filter { mail -> mail.deliveryMode == MailDeliveryMode.EXTERNAL && mail.ticket == null }

        supportMailsWithoutTicket.forEach { mail ->
            ticketLifecycleService.attachIncomingMail(mail)
            mailRepository.save(mail)
        }

        // Backfill internal mails without tickets
        val internalMailsWithoutTicket = mailRepository.findAllByStatus(MailStatus.SENT)
            .filter { mail -> mail.deliveryMode == MailDeliveryMode.INTERNAL && mail.ticket == null }

        // if there are internal mails without tickets, create a ticket for them
        internalMailsWithoutTicket.forEach { mail ->
            ticketLifecycleService.attachInternalMail(mail)
            mailRepository.save(mail)
        }

        // Log the number of backfilled tickets
        val backfilledTicketCount = supportMailsWithoutTicket.size + internalMailsWithoutTicket.size
        if (backfilledTicketCount > 0) {
            logger.info("Backfilled {} tickets for existing mails.", backfilledTicketCount)
        }
    }
}
