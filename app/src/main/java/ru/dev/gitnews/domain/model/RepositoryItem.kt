package ru.dev.gitnews.domain.model

data class RepositoryItem(
    val id: Long,
    val name: String,
    val fullName: String,
    val ownerLogin: String,
    val ownerAvatarUrl: String,
    val description: String?,
    val htmlUrl: String,
    val starsCount: Int,
    val forksCount: Int,
    val language: String?,
    val languageColor: String?,
    val topics: List<String> = emptyList(),
    val licenseName: String?,
    val createdAt: String? = null,
    val pushedAt: String?,
    val isStarred: Boolean = false,
    val isBookmarked: Boolean = false
) {
    val relativeTime: String
        get() = calculateRelativeTime(pushedAt)

    companion object {
        fun calculateRelativeTime(isoTimestamp: String?): String {
            if (isoTimestamp.isNullOrBlank()) return "недавно"
            return try {
                val instant = java.time.Instant.parse(isoTimestamp)
                val now = java.time.Instant.now()
                val diffSeconds = java.time.Duration.between(instant, now).seconds
                when {
                    diffSeconds < 60 -> "только что"
                    diffSeconds < 3600 -> "${diffSeconds / 60} мин назад"
                    diffSeconds < 86400 -> "${diffSeconds / 3600} ч назад"
                    diffSeconds < 604800 -> "${diffSeconds / 86400} дн назад"
                    diffSeconds < 2592000 -> "${diffSeconds / 604800} нед назад"
                    else -> "${diffSeconds / 2592000} мес назад"
                }
            } catch (e: Exception) {
                "недавно"
            }
        }

        fun getLanguageColor(language: String?): String {
            return when (language?.lowercase()) {
                "kotlin" -> "#A97BFF"
                "python" -> "#3572A5"
                "rust" -> "#DEA584"
                "javascript" -> "#F7DF1E"
                "typescript" -> "#3178C6"
                "java" -> "#B07219"
                "c++", "cpp" -> "#F34B7D"
                "c" -> "#555555"
                "go" -> "#00ADD8"
                "swift" -> "#F05138"
                "ruby" -> "#701516"
                "php" -> "#4F5D95"
                "c#" -> "#178600"
                "dart" -> "#00B4AB"
                "shell" -> "#89E051"
                else -> "#8A8F98"
            }
        }
    }
}
