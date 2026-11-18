package ru.dev.gitnews.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "presets")
data class PresetEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "query_keywords")
    val queryKeywords: String = "",

    @ColumnInfo(name = "languages_json")
    val languagesJson: String = "[]",

    @ColumnInfo(name = "min_stars")
    val minStars: Int = 0,

    @ColumnInfo(name = "max_stars")
    val maxStars: Int = 0,

    @ColumnInfo(name = "pushed_within_days")
    val pushedWithinDays: Int = 7,

    @ColumnInfo(name = "exclude_forks")
    val excludeForks: Boolean = true,

    @ColumnInfo(name = "sort_field")
    val sortField: String = "stars",

    @ColumnInfo(name = "sort_order")
    val sortOrder: String = "desc",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
