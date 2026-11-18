package ru.dev.gitnews.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.dev.gitnews.data.local.entity.BookmarkEntity

data class ReleaseUpdateEvent(
    val repoId: Long,
    val fullName: String,
    val newTag: String,
    val releaseName: String,
    val publishedAt: String,
    val htmlUrl: String
)

interface ReleaseTrackerRepository {
    fun getTrackedRepositoriesFlow(): Flow<List<BookmarkEntity>>
    suspend fun checkNewReleases(): List<ReleaseUpdateEvent>
    suspend fun updateReleaseSnapshot(repoId: Long, tag: String, name: String, publishedAt: String)
    fun isNotificationsEnabled(): Boolean
    fun setNotificationsEnabled(enabled: Boolean)
    fun schedulePeriodicCheck(enabled: Boolean)
}
