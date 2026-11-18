package ru.dev.gitnews.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.dev.gitnews.domain.model.Preset

interface PresetRepository {
    fun getPresetsFlow(): Flow<List<Preset>>
    suspend fun getPresetById(id: String): Preset?
    suspend fun createPreset(preset: Preset)
    suspend fun updatePreset(preset: Preset)
    suspend fun deletePreset(id: String)
    suspend fun getPresetsCount(): Int
    suspend fun exportPresetsJson(): String
    suspend fun importPresetsJson(jsonString: String): Result<Int>
}
