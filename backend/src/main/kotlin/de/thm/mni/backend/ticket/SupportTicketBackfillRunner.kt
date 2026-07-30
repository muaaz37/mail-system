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

        if (supportMailsWithoutTicket.isNotEmpty()) {
            logger.info("Backfilled {} support tickets for existing imported mails.", supportMailsWithoutTicket.size)
        }
    }
}
