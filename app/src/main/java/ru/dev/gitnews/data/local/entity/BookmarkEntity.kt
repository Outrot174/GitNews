package ru.dev.gitnews.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey
    @ColumnInfo(name = "repo_id")
    val repoId: Long,

    @ColumnInfo(name = "full_name")
    val fullName: String,

    @ColumnInfo(name = "owner_login")
    val ownerLogin: String,

    @ColumnInfo(name = "description")
    val description: String?,

    @ColumnInfo(name = "translated_description")
    val translatedDescription: String?,

    @ColumnInfo(name = "language")
    val language: String?,

    @ColumnInfo(name = "language_color")
    val languageColor: String?,

    @ColumnInfo(name = "stars_count")
    val starsCount: Int,

    @ColumnInfo(name = "forks_count")
    val forksCount: Int,

    @ColumnInfo(name = "html_url")
    val htmlUrl: String,

    @ColumnInfo(name = "pushed_at")
    val pushedAt: String?,

    @ColumnInfo(name = "cached_readme")
    val cachedReadme: String? = null,

    @ColumnInfo(name = "cached_translated_readme")
    val cachedTranslatedReadme: String? = null,

    @ColumnInfo(name = "bookmarked_at")
    val bookmarkedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "track_releases")
    val trackReleases: Boolean = true,

    @ColumnInfo(name = "last_known_release_tag")
    val lastKnownReleaseTag: String? = null,

    @ColumnInfo(name = "last_known_release_name")
    val lastKnownReleaseName: String? = null,

    @ColumnInfo(name = "last_release_published_at")
    val lastReleasePublishedAt: String? = null
)
