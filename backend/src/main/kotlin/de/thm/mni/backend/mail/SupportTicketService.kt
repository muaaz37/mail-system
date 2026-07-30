package de.thm.mni.backend.mail

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.security.SecureRandom

/**
 * Manages support ticket numbers in subjects and persisted mail records.
 */
@Service
class SupportTicketService(private val mailRepository: MailRepository) {
    private val random = SecureRandom()

    /**
     * Reads an existing ticket number from a mail subject.
     */
    fun extractTicketNumber(subject: String): String? {
        return TICKET_PATTERN.find(subject)?.groupValues?.get(1)?.uppercase()
    }

    /**
     * Reuses an existing ticket number or generates one for a new support case.
     */
    @Transactional
    fun ensureTicketNumber(mail: Mail): String {
        val existingTicket = mail.ticketNumber ?: extractTicketNumber(mail.subject)
        if (existingTicket != null) {
            mail.ticketNumber = existingTicket
            mailRepository.save(mail)
            return existingTicket
        }

        val generatedTicket = generateUniqueTicketNumber()
        mail.ticketNumber = generatedTicket
        mailRepository.save(mail)
        return generatedTicket
    }

    /**
     * Builds the subject used when answering an imported support mail.
     */
    fun buildReplySubject(originalSubject: String, ticketNumber: String): String {
        val replySubject = if (originalSubject.startsWith(REPLY_PREFIX, ignoreCase = true)) {
            originalSubject
        } else {
            "$REPLY_PREFIX $originalSubject"
        }
        return prependTicketIfMissing(replySubject, ticketNumber)
    }

    /**
     * Adds the ticket prefix only when the subject does not already contain one.
     */
    fun prependTicketIfMissing(subject: String, ticketNumber: String): String {
        if (extractTicketNumber(subject) != null) {
            return subject
        }
        return "[$ticketNumber] $subject".trim()
    }

    /**
     * Generates a random ticket number that is not yet present in stored mails.
     */
    fun generateUniqueTicketNumber(): String {
        var ticketNumber: String
        do {
            val number = random.nextInt(TICKET_BOUND).toString().padStart(TICKET_DIGITS, '0')
            ticketNumber = "$TICKET_PREFIX$number"
        } while (mailRepository.existsByTicketNumber(ticketNumber))
        return ticketNumber
    }

    fun removeTicketPrefix(subject: String): String {
        return subject.replace(Regex("^\\s*\\[$TICKET_PREFIX\\d{$TICKET_DIGITS}\\]\\s*", RegexOption.IGNORE_CASE), "")
            .trim()
    }

    private companion object {
        const val REPLY_PREFIX = "Re:"
        const val TICKET_PREFIX = "TICKET-"
        const val TICKET_DIGITS = 6
        const val TICKET_BOUND = 1_000_000
        val TICKET_PATTERN = Regex("\\b(TICKET-\\d{$TICKET_DIGITS})\\b", RegexOption.IGNORE_CASE)
    }
}
