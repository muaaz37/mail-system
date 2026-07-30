package de.thm.mni.backend.imap

import de.thm.mni.backend.imap.dto.ImapMailAttachment
import de.thm.mni.backend.imap.dto.ImapMailData
import de.thm.mni.backend.imap.dto.ImapMailPreview
import de.thm.mni.backend.smtp.SMTPService
import jakarta.mail.Address
import jakarta.mail.Message
import jakarta.mail.Multipart
import jakarta.mail.Part
import jakarta.mail.internet.AddressException
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeUtility
import org.springframework.stereotype.Component

/**
 * Converts JavaMail messages into application DTOs used by the import pipeline.
 */
@Component
class ImapMessageMapper {
    /**
     * Creates a small preview without reading attachments or changing IMAP flags.
     */
    fun toPreview(message: Message): ImapMailPreview {
        return ImapMailPreview(
            subject = message.subject ?: "",
            from = message.from?.firstOrNull()?.toString(),
            sentDate = message.sentDate,
            body = null,
            messageId = message.getHeader("Message-ID")?.firstOrNull()
        )
    }

    /**
     * Extracts sender, recipients, body and attachments from an unread IMAP message.
     */
    fun toMailData(message: Message): ImapMailData {
        return ImapMailData(
            subject = message.subject ?: "",
            from = message.from?.firstOrNull()?.toString(),
            to = extractAddresses(message.getRecipients(Message.RecipientType.TO)),
            cc = extractAddresses(message.getRecipients(Message.RecipientType.CC)),
            replyTo = extractAddresses(message.replyTo),
            sentDate = message.sentDate,
            body = extractBody(message),
            messageId = message.getHeader("Message-ID")?.firstOrNull(),
            systemGenerated = isSystemGenerated(message),
            attachments = extractAttachments(message)
        )
    }

    /**
     * Detects mails sent by this application so they are not imported again as support requests.
     */
    private fun isSystemGenerated(message: Message): Boolean {
        return message.getHeader(SMTPService.APP_ORIGIN_HEADER)
            ?.any { value -> value.equals(SMTPService.APP_ORIGIN_VALUE, ignoreCase = true) }
            ?: false
    }

    /**
     * Extracts the first readable plain text or HTML body while ignoring attachments.
     */
    private fun extractBody(part: Part): String? = when {
        part.isMimeType("text/plain") || part.isMimeType("text/html") -> extractTextBody(part)
        part.isMimeType("multipart/*") -> extractMultipartBody(part.content as Multipart)
        else -> null
    }

    private fun extractTextBody(part: Part): String? {
        return if (isAttachment(part)) {
            null
        } else {
            part.content as? String
        }
    }

    private fun extractMultipartBody(multipart: Multipart): String? {
        return (0 until multipart.count)
            .asSequence()
            .map { index -> multipart.getBodyPart(index) }
            .filterNot { bodyPart -> isAttachment(bodyPart) }
            .mapNotNull { bodyPart -> extractBody(bodyPart)?.takeIf { text -> text.isNotBlank() } }
            .firstOrNull()
    }

    /**
     * Recursively extracts attachment parts from nested MIME structures.
     */
    private fun extractAttachments(part: Part): List<ImapMailAttachment> = when {
        part.isMimeType("multipart/*") -> extractMultipartAttachments(part.content as Multipart)
        isAttachment(part) -> listOf(toAttachment(part))
        else -> emptyList()
    }

    private fun extractMultipartAttachments(multipart: Multipart): List<ImapMailAttachment> {
        return (0 until multipart.count).flatMap { index ->
            extractAttachments(multipart.getBodyPart(index))
        }
    }

    private fun toAttachment(part: Part): ImapMailAttachment {
        return ImapMailAttachment(
            fileName = decodeFileName(part.fileName),
            mimeType = part.contentType?.substringBefore(";")?.trim(),
            bytes = part.inputStream.readBytes()
        )
    }

    private fun isAttachment(part: Part): Boolean {
        val hasAttachmentDisposition = Part.ATTACHMENT.equals(part.disposition, ignoreCase = true)
        val hasFilename = !part.fileName.isNullOrBlank()
        return hasAttachmentDisposition || hasFilename
    }

    private fun decodeFileName(fileName: String?): String {
        return fileName
            ?.takeIf { name -> name.isNotBlank() }
            ?.let { name -> MimeUtility.decodeText(name) }
            ?: DEFAULT_ATTACHMENT_FILENAME
    }

    private companion object {
        const val DEFAULT_ATTACHMENT_FILENAME = "attachment"
    }
}

/**
 * Extracts plain email addresses from JavaMail address objects.
 */
private fun extractAddresses(addresses: Array<Address>?): List<String> {
    return addresses
        ?.mapNotNull { address -> address.emailAddress() }
        ?: emptyList()
}

/**
 * Normalizes a JavaMail address to a plain email address.
 */
private fun Address.emailAddress(): String? {
    val internetAddress = this as? InternetAddress
    if (internetAddress != null) {
        return internetAddress.address?.trim()?.takeIf { address -> address.isNotBlank() }
    }

    return try {
        InternetAddress.parse(toString(), false)
            .firstOrNull()
            ?.address
            ?.trim()
            ?.takeIf { address -> address.isNotBlank() }
    } catch (_: AddressException) {
        null
    }
}
