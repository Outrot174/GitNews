package ru.dev.gitnews.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import ru.dev.gitnews.data.local.dao.BookmarkDao
import ru.dev.gitnews.data.local.dao.PresetDao
import ru.dev.gitnews.data.local.dao.SearchHistoryDao
import ru.dev.gitnews.data.local.dao.TranslationCacheDao
import ru.dev.gitnews.data.local.entity.BookmarkEntity
import ru.dev.gitnews.data.local.entity.PresetEntity
import ru.dev.gitnews.data.local.entity.SearchHistoryEntity
import ru.dev.gitnews.data.local.entity.TranslationCacheEntity

@Database(
    entities = [
        PresetEntity::class,
        BookmarkEntity::class,
        TranslationCacheEntity::class,
        SearchHistoryEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class GitNewsDatabase : RoomDatabase() {

    abstract fun presetDao(): PresetDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun translationCacheDao(): TranslationCacheDao
    abstract fun searchHistoryDao(): SearchHistoryDao

    companion object {
        private const val DATABASE_NAME = "gitnews_app.db"

        @Volatile
        private var INSTANCE: GitNewsDatabase? = null

        fun getInstance(context: Context): GitNewsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GitNewsDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
