package de.thm.mni.backend.mail.external

/**
 * Extracts normalized RFC message identifiers from mail threading headers.
 */
fun String?.toMessageIdList(): List<String> {
    val value = this?.trim().orEmpty()
    val bracketedIds = MESSAGE_ID_PATTERN.findAll(value)
        .map { match -> match.value.trim() }
        .toList()

    return when {
        value.isBlank() -> emptyList()
        bracketedIds.isNotEmpty() -> bracketedIds.distinct()
        else -> value.split(MESSAGE_ID_SEPARATOR_PATTERN)
            .map { id -> id.trim() }
            .filter { id -> id.isNotBlank() }
            .distinct()
    }
}

/**
 * Stores message identifiers in the same whitespace-separated format used by the References header.
 */
fun List<String>.toMessageIdHeaderValue(): String {
    return flatMap { id -> id.toMessageIdList().ifEmpty { listOf(id.trim()) } }
        .filter { id -> id.isNotBlank() }
        .distinct()
        .joinToString(" ")
}

private val MESSAGE_ID_PATTERN = Regex("<[^<>\\s]+>")
private val MESSAGE_ID_SEPARATOR_PATTERN = Regex("[,\\s]+")
