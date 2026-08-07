package de.thm.mni.backend.mail

private const val DEFAULT_REPLY_SUBJECT = "Message"

private val REPLY_PREFIX_PATTERN = Regex(
    pattern = "^(?:\\s*Re\\s*:\\s*)+",
    option = RegexOption.IGNORE_CASE
)

/**
 * Builds a normalized subject for a regular mail reply.
 *
 * Existing reply prefixes are removed before exactly one prefix is added.
 */
fun buildReplySubject(originalSubject: String): String {
    val baseSubject = originalSubject
        .trim()
        .replace(REPLY_PREFIX_PATTERN, "")
        .trim()
        .ifBlank { DEFAULT_REPLY_SUBJECT }

    return "Re: $baseSubject"
}
