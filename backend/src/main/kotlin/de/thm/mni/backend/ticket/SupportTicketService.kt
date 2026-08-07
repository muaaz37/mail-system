package de.thm.mni.backend.ticket

import de.thm.mni.backend.mail.MailRepository
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
     * Builds the subject used when answering an imported support mail.
     */
    fun buildReplySubject(originalSubject: String, ticketNumber: String): String {
        return normalizeReplySubject(originalSubject, ticketNumber)
    }

    /**
     * Produces the canonical reply subject with exactly one ticket number and one reply prefix.
     */
    fun prependTicketIfMissing(subject: String, ticketNumber: String): String {
        return normalizeReplySubject(subject, ticketNumber)
    }

    /**
     * Normalizes a subject line to include exactly one ticket number and one reply prefix.
     */
    private fun normalizeReplySubject(subject: String, ticketNumber: String): String {
        val subjectWithoutTickets = subject.replace(TICKET_REFERENCE_PATTERN, " ").trim()
        val baseSubject = subjectWithoutTickets
            .replace(LEADING_REPLY_PREFIXES_PATTERN, "")
            .trim()
            .ifBlank { DEFAULT_SUPPORT_SUBJECT }
        return "[$ticketNumber] $REPLY_PREFIX $baseSubject".trim()
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

    /**
     * Removes a ticket number prefix from a subject, if present.
     */
    fun removeTicketPrefix(subject: String): String {
        return subject.replace(Regex("^\\s*\\[$TICKET_PREFIX\\d{$TICKET_DIGITS}]\\s*", RegexOption.IGNORE_CASE), "")
            .trim()
    }

    /**
     * Removes the reply prefix from a subject, if present.
     */
    private companion object {
        const val REPLY_PREFIX = "Re:"
        const val TICKET_PREFIX = "TICKET-"
        const val DEFAULT_SUPPORT_SUBJECT = "Support request"
        const val TICKET_DIGITS = 6
        const val TICKET_BOUND = 1_000_000
        val TICKET_PATTERN = Regex("\\b(TICKET-\\d{$TICKET_DIGITS})\\b", RegexOption.IGNORE_CASE)
        val TICKET_REFERENCE_PATTERN = Regex("\\[?\\s*TICKET-\\d{$TICKET_DIGITS}\\s*]?", RegexOption.IGNORE_CASE)
        val LEADING_REPLY_PREFIXES_PATTERN = Regex("^(?:\\s*Re\\s*:\\s*)+", RegexOption.IGNORE_CASE)
    }
}
