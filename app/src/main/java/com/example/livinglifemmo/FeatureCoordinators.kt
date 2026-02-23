package com.example.livinglifemmo

object CommunityCoordinator {
    private val blockedTokens = listOf("<script", "javascript:", "data:text/html", "file://", "intent://")

    fun sanitizeDisplayName(raw: String): String {
        val compact = raw.trim().replace(Regex("\\s+"), " ")
        return compact.ifBlank { "Player" }.take(24)
    }

    fun sanitizeCommunityText(raw: String, maxLen: Int): String {
        val noControlChars = raw.replace(Regex("[\\p{Cntrl}&&[^\n\t]]"), " ")
        val compact = noControlChars.trim().replace(Regex("\\s+"), " ")
        return compact.take(maxLen)
    }

    fun sanitizeTags(tags: List<String>): List<String> {
        return tags.asSequence()
            .map { sanitizeCommunityText(it.lowercase(), 20) }
            .filter { it.isNotBlank() }
            .distinct()
            .take(6)
            .toList()
    }

    fun hasSuspiciousContent(raw: String): Boolean {
        val lower = raw.lowercase()
        return blockedTokens.any { lower.contains(it) }
    }
}
