package ru.dev.gitnews.ui.screens.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.dev.gitnews.domain.model.BuiltInFeedType
import ru.dev.gitnews.domain.model.FeedSortOption
import ru.dev.gitnews.domain.model.Preset
import ru.dev.gitnews.domain.model.RepositoryItem
import ru.dev.gitnews.domain.repository.BookmarkRepository
import ru.dev.gitnews.domain.repository.GitHubRepository
import ru.dev.gitnews.domain.repository.PresetRepository
import ru.dev.gitnews.ml.MlKitTranslationManager

enum class TranslationMode {
    ALWAYS,
    ON_REQUEST,
    OFF
}

class FeedViewModel(
    private val gitHubRepository: GitHubRepository,
    private val presetRepository: PresetRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val translationManager: MlKitTranslationManager
) : ViewModel() {

    val presets: StateFlow<List<Preset>> = presetRepository.getPresetsFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _selectedBuiltIn = MutableStateFlow<BuiltInFeedType?>(BuiltInFeedType.TRENDING)
    val selectedBuiltIn: StateFlow<BuiltInFeedType?> = _selectedBuiltIn.asStateFlow()

    private val _selectedPreset = MutableStateFlow<Preset?>(null)
    val selectedPreset: StateFlow<Preset?> = _selectedPreset.asStateFlow()

    private val _currentSort = MutableStateFlow(FeedSortOption.STARS_DESC)
    val currentSort: StateFlow<FeedSortOption> = _currentSort.asStateFlow()

    private val _items = MutableStateFlow<List<RepositoryItem>>(emptyList())
    val items: StateFlow<List<RepositoryItem>> = _items.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var currentPage = 1

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _endReached = MutableStateFlow(false)
    val endReached: StateFlow<Boolean> = _endReached.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _translationMode = MutableStateFlow(TranslationMode.ALWAYS)
    val translationMode: StateFlow<TranslationMode> = _translationMode.asStateFlow()

    private val _translatedDescriptions = MutableStateFlow<Map<Long, String>>(emptyMap())
    val translatedDescriptions: StateFlow<Map<Long, String>> = _translatedDescriptions.asStateFlow()

    init {
        loadFeed(isInitial = true)
    }

    fun selectBuiltIn(type: BuiltInFeedType) {
        _selectedPreset.value = null
        _selectedBuiltIn.value = type
        loadFeed(isInitial = false)
    }

    fun selectPreset(preset: Preset?) {
        if (preset == null) {
            selectBuiltIn(BuiltInFeedType.TRENDING)
        } else {
            _selectedBuiltIn.value = null
            _selectedPreset.value = preset
            loadFeed(isInitial = false)
        }
    }

    fun setSortOption(sortOption: FeedSortOption) {
        _currentSort.value = sortOption
        if (sortOption.serverSort != null) {
            loadFeed(isInitial = false)
        } else {
            _items.value = applyClientSort(_items.value, sortOption)
        }
    }

    fun setTranslationMode(mode: TranslationMode) {
        _translationMode.value = mode
        if (mode == TranslationMode.ALWAYS) {
            translateVisibleItems()
        }
    }

    fun refreshFeed() {
        _isRefreshing.value = true
        loadFeed(isInitial = false)
    }

    fun createPreset(preset: Preset) {
        viewModelScope.launch {
            presetRepository.createPreset(preset)
            _selectedBuiltIn.value = null
            _selectedPreset.value = preset
            loadFeed(isInitial = false)
        }
    }

    fun deletePreset(id: String) {
        viewModelScope.launch {
            presetRepository.deletePreset(id)
            if (_selectedPreset.value?.id == id) {
                _selectedPreset.value = null
                _selectedBuiltIn.value = BuiltInFeedType.TRENDING
                loadFeed(isInitial = false)
            }
        }
    }

    fun toggleStar(item: RepositoryItem) {
        val currentStarred = item.isStarred
        val newStarred = !currentStarred

        updateItemInState(item.id) {
            it.copy(
                isStarred = newStarred,
                starsCount = if (newStarred) it.starsCount + 1 else (it.starsCount - 1).coerceAtLeast(0)
            )
        }

        viewModelScope.launch {
            val result = if (newStarred) {
                gitHubRepository.starRepository(item.ownerLogin, item.name)
            } else {
                gitHubRepository.unstarRepository(item.ownerLogin, item.name)
            }

            result.onFailure {
                updateItemInState(item.id) {
                    it.copy(
                        isStarred = currentStarred,
                        starsCount = if (currentStarred) it.starsCount + 1 else (it.starsCount - 1).coerceAtLeast(0)
                    )
                }
            }
        }
    }

    fun toggleBookmark(item: RepositoryItem) {
        viewModelScope.launch {
            val translatedDesc = _translatedDescriptions.value[item.id]
            val isNowBookmarked = bookmarkRepository.toggleBookmark(item, translatedDesc)
            updateItemInState(item.id) {
                it.copy(isBookmarked = isNowBookmarked)
            }
        }
    }

    fun requestTranslation(repoId: Long, rawDescription: String) {
        if (rawDescription.isBlank()) return
        viewModelScope.launch {
            val translated = translationManager.translateText(rawDescription)
            _translatedDescriptions.value = _translatedDescriptions.value + (repoId to translated)
        }
    }

    private fun loadFeed(isInitial: Boolean) {
        viewModelScope.launch {
            if (isInitial) _isLoading.value = true
            _errorMessage.value = null
            currentPage = 1
            _endReached.value = false

            val preset = _selectedPreset.value
            val builtIn = _selectedBuiltIn.value
            val query = when {
                preset != null -> preset.toGitHubQuery()
                builtIn == BuiltInFeedType.RECENT -> Preset.createRecentlyAddedQuery(7)
                else -> Preset.createGlobalTrendingQuery(7)
            }

            val sort = _currentSort.value.serverSort ?: preset?.sortField ?: "stars"
            val order = _currentSort.value.serverOrder ?: preset?.sortOrder ?: "desc"

            val result = gitHubRepository.searchRepositories(
                query = query,
                sort = sort,
                order = order,
                page = 1,
                perPage = 30
            )

            result.onSuccess { repoList ->
                if (repoList.isEmpty() || repoList.size < 30) {
                    _endReached.value = true
                }
                val enrichedList = repoList.map { repo ->
                    val isBookmarked = bookmarkRepository.isBookmarked(repo.id)
                    repo.copy(isBookmarked = isBookmarked)
                }
                _items.value = applyClientSort(enrichedList, _currentSort.value)
                _isLoading.value = false
                _isRefreshing.value = false

                if (_translationMode.value == TranslationMode.ALWAYS) {
                    translateVisibleItems()
                }
            }.onFailure { e ->
                _isLoading.value = false
                _isRefreshing.value = false
                _errorMessage.value = e.localizedMessage ?: "Ошибка загрузки ленты"
            }
        }
    }

    fun loadNextPage() {
        if (_isLoading.value || _isLoadingMore.value || _endReached.value) return

        viewModelScope.launch {
            _isLoadingMore.value = true
            val nextPage = currentPage + 1
            val preset = _selectedPreset.value
            val builtIn = _selectedBuiltIn.value
            val query = when {
                preset != null -> preset.toGitHubQuery()
                builtIn == BuiltInFeedType.RECENT -> Preset.createRecentlyAddedQuery(7)
                else -> Preset.createGlobalTrendingQuery(7)
            }

            val sort = _currentSort.value.serverSort ?: preset?.sortField ?: "stars"
            val order = _currentSort.value.serverOrder ?: preset?.sortOrder ?: "desc"

            val result = gitHubRepository.searchRepositories(
                query = query,
                sort = sort,
                order = order,
                page = nextPage,
                perPage = 30
            )

            result.onSuccess { repoList ->
                if (repoList.isEmpty() || repoList.size < 30) {
                    _endReached.value = true
                }
                val enrichedList = repoList.map { repo ->
                    val isBookmarked = bookmarkRepository.isBookmarked(repo.id)
                    repo.copy(isBookmarked = isBookmarked)
                }
                _items.value = applyClientSort(_items.value + enrichedList, _currentSort.value)
                currentPage = nextPage
                _isLoadingMore.value = false

                if (_translationMode.value == TranslationMode.ALWAYS) {
                    translateVisibleItems()
                }
            }.onFailure {
                _isLoadingMore.value = false
            }
        }
    }

    private fun applyClientSort(list: List<RepositoryItem>, sortOption: FeedSortOption): List<RepositoryItem> {
        return when (sortOption) {
            FeedSortOption.NAME_ASC -> list.sortedBy { it.fullName.lowercase() }
            FeedSortOption.NAME_DESC -> list.sortedByDescending { it.fullName.lowercase() }
            FeedSortOption.CREATED_DESC -> list.sortedByDescending { it.createdAt ?: "" }
            FeedSortOption.CREATED_ASC -> list.sortedBy { it.createdAt ?: "" }
            else -> list
        }
    }

    private fun translateVisibleItems() {
        val currentItems = _items.value
        viewModelScope.launch {
            val currentMap = _translatedDescriptions.value.toMutableMap()
            for (repo in currentItems) {
                val desc = repo.description
                if (!desc.isNullOrBlank() && !currentMap.containsKey(repo.id)) {
                    val translated = translationManager.translateText(desc)
                    currentMap[repo.id] = translated
                    _translatedDescriptions.value = currentMap.toMap()
                }
            }
        }
    }

    private fun updateItemInState(repoId: Long, transform: (RepositoryItem) -> RepositoryItem) {
        _items.value = _items.value.map {
            if (it.id == repoId) transform(it) else it
        }
    }
}
