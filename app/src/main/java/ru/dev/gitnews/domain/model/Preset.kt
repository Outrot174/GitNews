package ru.dev.gitnews.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.dev.gitnews.data.local.entity.PresetEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

@Serializable
data class Preset(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val queryKeywords: String = "",
    val languages: List<String> = emptyList(),
    val minStars: Int = 0,
    val maxStars: Int = 0,
    val pushedWithinDays: Int = 7,
    val excludeForks: Boolean = true,
    val sortField: String = "stars",
    val sortOrder: String = "desc",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toGitHubQuery(): String {
        val parts = mutableListOf<String>()

        // 1. Keywords
        if (queryKeywords.isNotBlank()) {
            val trimmed = queryKeywords.trim()
            if (trimmed.contains(",")) {
                val orKeywords = trimmed.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .joinToString(" OR ") { if (it.contains(" ")) "\"$it\"" else it }
                if (orKeywords.isNotEmpty()) {
                    parts.add("($orKeywords)")
                }
            } else {
                parts.add(if (trimmed.contains(" ")) "\"$trimmed\"" else trimmed)
            }
        }

        // 2. Languages
        if (languages.isNotEmpty()) {
            if (languages.size == 1) {
                parts.add("language:${languages.first()}")
            } else {
                val orLangs = languages.joinToString(" OR ") { "language:$it" }
                parts.add("($orLangs)")
            }
        }

        // 3. Stars
        when {
            minStars > 0 && maxStars > 0 && maxStars >= minStars -> {
                parts.add("stars:$minStars..$maxStars")
            }
            minStars > 0 -> {
                parts.add("stars:>=$minStars")
            }
            maxStars > 0 -> {
                parts.add("stars:<=$maxStars")
            }
        }

        // 4. Pushed within days
        if (pushedWithinDays > 0) {
            val sinceDate = LocalDate.now().minusDays(pushedWithinDays.toLong())
            val dateStr = sinceDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
            parts.add("pushed:>=$dateStr")
        }

        // 5. Forks
        if (excludeForks) {
            parts.add("fork:false")
        }

        return if (parts.isEmpty()) {
            // Default fallback query
            "stars:>50 fork:false"
        } else {
            parts.joinToString(" ")
        }
    }

    fun toEntity(): PresetEntity {
        return PresetEntity(
            id = id,
            title = title,
            queryKeywords = queryKeywords,
            languagesJson = Json.encodeToString(languages),
            minStars = minStars,
            maxStars = maxStars,
            pushedWithinDays = pushedWithinDays,
            excludeForks = excludeForks,
            sortField = sortField,
            sortOrder = sortOrder,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromEntity(entity: PresetEntity): Preset {
            val parsedLanguages = try {
                Json.decodeFromString<List<String>>(entity.languagesJson)
            } catch (e: Exception) {
                emptyList()
            }
            return Preset(
                id = entity.id,
                title = entity.title,
                queryKeywords = entity.queryKeywords,
                languages = parsedLanguages,
                minStars = entity.minStars,
                maxStars = entity.maxStars,
                pushedWithinDays = entity.pushedWithinDays,
                excludeForks = entity.excludeForks,
                sortField = entity.sortField,
                sortOrder = entity.sortOrder,
                createdAt = entity.createdAt
            )
        }

        fun createGlobalTrendingQuery(days: Int = 7): String {
            val sinceDate = LocalDate.now().minusDays(days.toLong())
            val dateStr = sinceDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
            return "stars:>50 pushed:>=$dateStr fork:false"
        }

        fun createRecentlyAddedQuery(days: Int = 7): String {
            val sinceDate = LocalDate.now().minusDays(days.toLong())
            val dateStr = sinceDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
            return "created:>=$dateStr stars:>10 fork:false"
        }
    }
}
