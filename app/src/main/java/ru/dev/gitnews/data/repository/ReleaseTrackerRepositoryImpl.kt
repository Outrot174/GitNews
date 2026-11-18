package ru.dev.gitnews.data.repository

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.dev.gitnews.data.local.SecureTokenStorage
import ru.dev.gitnews.data.local.dao.BookmarkDao
import ru.dev.gitnews.data.local.entity.BookmarkEntity
import ru.dev.gitnews.data.remote.GitHubApiService
import ru.dev.gitnews.domain.repository.ReleaseTrackerRepository
import ru.dev.gitnews.domain.repository.ReleaseUpdateEvent
import ru.dev.gitnews.worker.ReleaseCheckWorker
import java.util.concurrent.TimeUnit

class ReleaseTrackerRepositoryImpl(
    private val context: Context,
    private val gitHubApiService: GitHubApiService,
    private val bookmarkDao: BookmarkDao,
    private val secureTokenStorage: SecureTokenStorage
) : ReleaseTrackerRepository {

    override fun getTrackedRepositoriesFlow(): Flow<List<BookmarkEntity>> {
        return bookmarkDao.getAllBookmarks().map { list ->
            list.filter { it.trackReleases }
        }
    }

    override suspend fun checkNewReleases(): List<ReleaseUpdateEvent> {
        val tracked = bookmarkDao.getTrackedBookmarks()
        if (tracked.isEmpty()) return emptyList()

        val events = mutableListOf<ReleaseUpdateEvent>()
        for (repo in tracked) {
            try {
                val repoName = if (repo.fullName.contains('/')) {
                    repo.fullName.substringAfter('/')
                } else {
                    repo.fullName
                }
                val latest = gitHubApiService.getLatestRelease(
                    owner = repo.ownerLogin,
                    repo = repoName
                )
                val newTag = latest.tagName
                val releaseName = latest.name ?: latest.tagName
                val publishedAt = latest.publishedAt ?: ""

                if (repo.lastKnownReleaseTag == null) {
                    // Initialize first snapshot without false-alarm notification
                    bookmarkDao.updateReleaseSnapshot(
                        repoId = repo.repoId,
                        tag = newTag,
                        name = releaseName,
                        publishedAt = publishedAt
                    )
                } else if (repo.lastKnownReleaseTag != newTag) {
                    // Real new release found
                    bookmarkDao.updateReleaseSnapshot(
                        repoId = repo.repoId,
                        tag = newTag,
                        name = releaseName,
                        publishedAt = publishedAt
                    )
                    events.add(
                        ReleaseUpdateEvent(
                            repoId = repo.repoId,
                            fullName = repo.fullName,
                            newTag = newTag,
                            releaseName = releaseName,
                            publishedAt = publishedAt,
                            htmlUrl = latest.htmlUrl ?: "https://github.com/${repo.fullName}/releases"
                        )
                    )
                }
            } catch (e: Exception) {
                Log.d(TAG, "checkNewReleases skipped for ${repo.fullName}: ${e.message}")
            }
        }
        return events
    }

    override suspend fun updateReleaseSnapshot(
        repoId: Long,
        tag: String,
        name: String,
        publishedAt: String
    ) {
        bookmarkDao.updateReleaseSnapshot(repoId, tag, name, publishedAt)
    }

    override fun isNotificationsEnabled(): Boolean {
        return secureTokenStorage.isReleaseNotificationsEnabled()
    }

    override fun setNotificationsEnabled(enabled: Boolean) {
        secureTokenStorage.setReleaseNotificationsEnabled(enabled)
        schedulePeriodicCheck(enabled)
    }

    override fun schedulePeriodicCheck(enabled: Boolean) {
        try {
            val workManager = WorkManager.getInstance(context)
            if (enabled) {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val workRequest = PeriodicWorkRequestBuilder<ReleaseCheckWorker>(12, TimeUnit.HOURS)
                    .setConstraints(constraints)
                    .build()

                workManager.enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    workRequest
                )
                Log.d(TAG, "Periodic release check enqueued (12h interval)")
            } else {
                workManager.cancelUniqueWork(WORK_NAME)
                Log.d(TAG, "Periodic release check canceled")
            }
        } catch (e: Exception) {
            Log.e(TAG, "schedulePeriodicCheck error", e)
        }
    }

    companion object {
        private const val TAG = "ReleaseTrackerRepo"
        const val WORK_NAME = "gitnews_release_checker_work"
    }
}
