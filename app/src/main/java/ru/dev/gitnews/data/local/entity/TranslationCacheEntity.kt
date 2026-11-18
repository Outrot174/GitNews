package ru.dev.gitnews.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "translation_cache")
data class TranslationCacheEntity(
    @PrimaryKey
    @ColumnInfo(name = "cache_key")
    val cacheKey: String,

    @ColumnInfo(name = "source_lang")
    val sourceLang: String,

    @ColumnInfo(name = "target_lang")
    val targetLang: String = "ru",

    @ColumnInfo(name = "translated_text")
    val translatedText: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
