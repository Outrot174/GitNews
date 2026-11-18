package ru.dev.gitnews.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.dev.gitnews.domain.model.Bookmark
import ru.dev.gitnews.domain.model.RepositoryItem

interface BookmarkRepository {
    fun getBookmarksFlow(): Flow<List<Bookmark>>
    suspend fun isBookmarked(repoId: Long): Boolean
    fun isBookmarkedFlow(repoId: Long): Flow<Boolean>
    suspend fun toggleBookmark(item: RepositoryItem, translatedDescription: String? = null): Boolean
    suspend fun removeBookmark(repoId: Long)
    suspend fun getBookmark(repoId: Long): Bookmark?
    suspend fun updateCachedReadme(repoId: Long, rawReadme: String?, translatedReadme: String?)
}
