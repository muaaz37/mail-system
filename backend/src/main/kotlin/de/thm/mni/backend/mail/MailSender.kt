package de.thm.mni.backend.mail

/**
 * Sends an external mail independently of the underlying transport.
 */
interface MailSender {
    fun send(mail: Mail): Boolean
}
