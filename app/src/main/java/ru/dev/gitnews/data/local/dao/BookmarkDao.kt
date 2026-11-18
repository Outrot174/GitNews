package ru.dev.gitnews.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.dev.gitnews.data.local.entity.BookmarkEntity

@Dao
interface BookmarkDao {

    @Query("SELECT * FROM bookmarks ORDER BY bookmarked_at DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE repo_id = :repoId LIMIT 1")
    suspend fun getBookmarkById(repoId: Long): BookmarkEntity?

    @Query("SELECT * FROM bookmarks WHERE repo_id = :repoId LIMIT 1")
    fun getBookmarkByIdFlow(repoId: Long): Flow<BookmarkEntity?>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE repo_id = :repoId)")
    suspend fun isBookmarked(repoId: Long): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE repo_id = :repoId)")
    fun isBookmarkedFlow(repoId: Long): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE repo_id = :repoId")
    suspend fun deleteBookmarkById(repoId: Long)

    @Query("UPDATE bookmarks SET last_known_release_tag = :tag, last_known_release_name = :name, last_release_published_at = :publishedAt WHERE repo_id = :repoId")
    suspend fun updateReleaseSnapshot(repoId: Long, tag: String, name: String, publishedAt: String)

    @Query("SELECT * FROM bookmarks WHERE track_releases = 1")
    suspend fun getTrackedBookmarks(): List<BookmarkEntity>

    @Query("SELECT COUNT(*) FROM bookmarks")
    suspend fun count(): Int
}
