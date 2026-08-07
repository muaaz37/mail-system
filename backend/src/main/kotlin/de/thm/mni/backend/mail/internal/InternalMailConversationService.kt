package de.thm.mni.backend.mail.internal

import de.thm.mni.backend.error.ResourceNotFoundException
import de.thm.mni.backend.mail.Mail
import de.thm.mni.backend.mail.MailRepository
import de.thm.mni.backend.mail.enums.MailDeliveryMode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.ArrayDeque
import java.util.UUID

/**
 * Reconstructs internal conversations without creating support tickets.
 */
@Service
class InternalMailConversationService(
    private val mailRepository: MailRepository
) {
    /**
     * Returns the root internal mail and every reply in chronological order.
     */
    @Transactional(readOnly = true)
    fun getConversation(mail: Mail): List<Mail> {
        if (mail.deliveryMode != MailDeliveryMode.INTERNAL) {
            throw ResourceNotFoundException("Internal mail conversation not found")
        }

        val pending = ArrayDeque<Mail>()
        val visited = mutableSetOf<UUID>()
        val conversation = mutableListOf<Mail>()
        pending.add(findRoot(mail))

        while (pending.isNotEmpty()) {
            val current = pending.removeFirst()
            val currentId = current.id
            if (currentId != null && visited.add(currentId)) {
                conversation.add(current)
                mailRepository.findAllByInReplyToMail(current).forEach(pending::addLast)
            }
        }

        return conversation.sortedBy { item -> item.sentAt ?: item.createdAt }
    }

    /**
     * Resolves the first message and protects corrupted data from cyclic traversal.
     */
    private fun findRoot(mail: Mail): Mail {
        var current = mail
        val visited = mutableSetOf<UUID>()

        while (current.inReplyToMail != null && isUnvisited(current, visited)) {
            current = current.inReplyToMail!!
        }

        return current
    }

    private fun isUnvisited(mail: Mail, visited: MutableSet<UUID>): Boolean {
        val mailId = mail.id ?: return false
        return visited.add(mailId)
    }
}
