package ru.dev.gitnews.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.dev.gitnews.data.local.dao.PresetDao
import ru.dev.gitnews.domain.model.Preset
import ru.dev.gitnews.domain.repository.PresetRepository

class PresetRepositoryImpl(
    private val presetDao: PresetDao
) : PresetRepository {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    override fun getPresetsFlow(): Flow<List<Preset>> {
        return presetDao.getAllPresets().map { list ->
            list.map { Preset.fromEntity(it) }
        }
    }

    override suspend fun getPresetById(id: String): Preset? {
        val entity = presetDao.getPresetById(id) ?: return null
        return Preset.fromEntity(entity)
    }

    override suspend fun createPreset(preset: Preset) {
        presetDao.insertPreset(preset.toEntity())
    }

    override suspend fun updatePreset(preset: Preset) {
        presetDao.updatePreset(preset.toEntity())
    }

    override suspend fun deletePreset(id: String) {
        presetDao.deletePresetById(id)
    }

    override suspend fun getPresetsCount(): Int {
        return presetDao.count()
    }

    override suspend fun exportPresetsJson(): String {
        val presets = presetDao.getAllPresetsList().map { Preset.fromEntity(it) }
        return json.encodeToString(presets)
    }

    override suspend fun importPresetsJson(jsonString: String): Result<Int> {
        return runCatching {
            val list = json.decodeFromString<List<Preset>>(jsonString)
            if (list.isEmpty()) return@runCatching 0
            list.forEach { preset ->
                presetDao.insertPreset(preset.toEntity())
            }
            list.size
        }
    }
}
