package ru.dev.gitnews.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.dev.gitnews.data.local.entity.TranslationCacheEntity

@Dao
interface TranslationCacheDao {

    @Query("SELECT * FROM translation_cache WHERE cache_key = :cacheKey LIMIT 1")
    suspend fun getTranslation(cacheKey: String): TranslationCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranslation(entity: TranslationCacheEntity)

    @Query("DELETE FROM translation_cache WHERE created_at < :cutoffTimestamp")
    suspend fun clearOldTranslations(cutoffTimestamp: Long)

    @Query("DELETE FROM translation_cache")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM translation_cache")
    suspend fun count(): Int
}
