package ru.dev.gitnews.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.dev.gitnews.data.local.entity.SearchHistoryEntity

@Dao
interface SearchHistoryDao {

    @Query("SELECT query FROM search_history ORDER BY timestamp DESC LIMIT 7")
    fun getRecentQueriesFlow(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuery(entity: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE query = :query")
    suspend fun deleteQuery(query: String)

    @Query("DELETE FROM search_history")
    suspend fun clearHistory()
}
