package ru.dev.gitnews.domain.model

import ru.dev.gitnews.data.local.entity.BookmarkEntity

data class Bookmark(
    val repoId: Long,
    val fullName: String,
    val ownerLogin: String,
    val description: String?,
    val translatedDescription: String?,
    val language: String?,
    val languageColor: String?,
    val starsCount: Int,
    val forksCount: Int,
    val htmlUrl: String,
    val pushedAt: String?,
    val cachedReadme: String? = null,
    val cachedTranslatedReadme: String? = null,
    val bookmarkedAt: Long = System.currentTimeMillis(),
    val trackReleases: Boolean = true,
    val lastKnownReleaseTag: String? = null,
    val lastKnownReleaseName: String? = null,
    val lastReleasePublishedAt: String? = null
) {
    fun toRepositoryItem(): RepositoryItem {
        return RepositoryItem(
            id = repoId,
            name = fullName.substringAfter("/", fullName),
            fullName = fullName,
            ownerLogin = ownerLogin,
            ownerAvatarUrl = "",
            description = translatedDescription ?: description,
            htmlUrl = htmlUrl,
            starsCount = starsCount,
            forksCount = forksCount,
            language = language,
            languageColor = languageColor,
            topics = emptyList(),
            licenseName = null,
            pushedAt = pushedAt,
            isStarred = false,
            isBookmarked = true
        )
    }

    fun toEntity(): BookmarkEntity {
        return BookmarkEntity(
            repoId = repoId,
            fullName = fullName,
            ownerLogin = ownerLogin,
            description = description,
            translatedDescription = translatedDescription,
            language = language,
            languageColor = languageColor,
            starsCount = starsCount,
            forksCount = forksCount,
            htmlUrl = htmlUrl,
            pushedAt = pushedAt,
            cachedReadme = cachedReadme,
            cachedTranslatedReadme = cachedTranslatedReadme,
            bookmarkedAt = bookmarkedAt,
            trackReleases = trackReleases,
            lastKnownReleaseTag = lastKnownReleaseTag,
            lastKnownReleaseName = lastKnownReleaseName,
            lastReleasePublishedAt = lastReleasePublishedAt
        )
    }

    companion object {
        fun fromEntity(entity: BookmarkEntity): Bookmark {
            return Bookmark(
                repoId = entity.repoId,
                fullName = entity.fullName,
                ownerLogin = entity.ownerLogin,
                description = entity.description,
                translatedDescription = entity.translatedDescription,
                language = entity.language,
                languageColor = entity.languageColor,
                starsCount = entity.starsCount,
                forksCount = entity.forksCount,
                htmlUrl = entity.htmlUrl,
                pushedAt = entity.pushedAt,
                cachedReadme = entity.cachedReadme,
                cachedTranslatedReadme = entity.cachedTranslatedReadme,
                bookmarkedAt = entity.bookmarkedAt,
                trackReleases = entity.trackReleases,
                lastKnownReleaseTag = entity.lastKnownReleaseTag,
                lastKnownReleaseName = entity.lastKnownReleaseName,
                lastReleasePublishedAt = entity.lastReleasePublishedAt
            )
        }

        fun fromRepositoryItem(item: RepositoryItem, translatedDescription: String? = null): Bookmark {
            return Bookmark(
                repoId = item.id,
                fullName = item.fullName,
                ownerLogin = item.ownerLogin,
                description = item.description,
                translatedDescription = translatedDescription,
                language = item.language,
                languageColor = item.languageColor,
                starsCount = item.starsCount,
                forksCount = item.forksCount,
                htmlUrl = item.htmlUrl,
                pushedAt = item.pushedAt,
                trackReleases = true
            )
        }
    }
}
