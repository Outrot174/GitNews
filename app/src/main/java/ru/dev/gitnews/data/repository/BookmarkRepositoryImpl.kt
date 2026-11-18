package ru.dev.gitnews.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.dev.gitnews.data.local.dao.BookmarkDao
import ru.dev.gitnews.domain.model.Bookmark
import ru.dev.gitnews.domain.model.RepositoryItem
import ru.dev.gitnews.domain.repository.BookmarkRepository

class BookmarkRepositoryImpl(
    private val bookmarkDao: BookmarkDao
) : BookmarkRepository {

    override fun getBookmarksFlow(): Flow<List<Bookmark>> {
        return bookmarkDao.getAllBookmarks().map { list ->
            list.map { Bookmark.fromEntity(it) }
        }
    }

    override suspend fun isBookmarked(repoId: Long): Boolean {
        return bookmarkDao.isBookmarked(repoId)
    }

    override fun isBookmarkedFlow(repoId: Long): Flow<Boolean> {
        return bookmarkDao.isBookmarkedFlow(repoId)
    }

    override suspend fun toggleBookmark(
        item: RepositoryItem,
        translatedDescription: String?
    ): Boolean {
        val exists = bookmarkDao.isBookmarked(item.id)
        if (exists) {
            bookmarkDao.deleteBookmarkById(item.id)
            return false
        } else {
            val bookmark = Bookmark.fromRepositoryItem(item, translatedDescription)
            bookmarkDao.insertBookmark(bookmark.toEntity())
            return true
        }
    }

    override suspend fun removeBookmark(repoId: Long) {
        bookmarkDao.deleteBookmarkById(repoId)
    }

    override suspend fun getBookmark(repoId: Long): Bookmark? {
        val entity = bookmarkDao.getBookmarkById(repoId) ?: return null
        return Bookmark.fromEntity(entity)
    }

    override suspend fun updateCachedReadme(
        repoId: Long,
        rawReadme: String?,
        translatedReadme: String?
    ) {
        val existing = bookmarkDao.getBookmarkById(repoId) ?: return
        val updated = existing.copy(
            cachedReadme = rawReadme ?: existing.cachedReadme,
            cachedTranslatedReadme = translatedReadme ?: existing.cachedTranslatedReadme
        )
        bookmarkDao.insertBookmark(updated)
    }
}
