package ru.dev.gitnews.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.dev.gitnews.data.local.dao.SearchHistoryDao
import ru.dev.gitnews.data.local.entity.SearchHistoryEntity
import ru.dev.gitnews.domain.model.Preset
import ru.dev.gitnews.domain.model.RepositoryItem
import ru.dev.gitnews.domain.repository.BookmarkRepository
import ru.dev.gitnews.domain.repository.GitHubRepository
import ru.dev.gitnews.domain.repository.PresetRepository

class SearchViewModel(
    private val gitHubRepository: GitHubRepository,
    private val presetRepository: PresetRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val searchHistoryDao: SearchHistoryDao
) : ViewModel() {

    val recentQueries: StateFlow<List<String>> = searchHistoryDao.getRecentQueriesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val quickLanguages = listOf("Kotlin", "Python", "Rust", "TypeScript", "C++", "Go")

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _selectedLanguage = MutableStateFlow<String?>(null)
    val selectedLanguage: StateFlow<String?> = _selectedLanguage.asStateFlow()

    private val _minStars = MutableStateFlow(0)
    val minStars: StateFlow<Int> = _minStars.asStateFlow()

    private val _pushedDays = MutableStateFlow(30)
    val pushedDays: StateFlow<Int> = _pushedDays.asStateFlow()

    private val _noForks = MutableStateFlow(true)
    val noForks: StateFlow<Boolean> = _noForks.asStateFlow()

    private val _results = MutableStateFlow<List<RepositoryItem>>(emptyList())
    val results: StateFlow<List<RepositoryItem>> = _results.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var currentPage = 1

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _endReached = MutableStateFlow(false)
    val endReached: StateFlow<Boolean> = _endReached.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var searchDebounceJob: Job? = null

    fun onQueryChanged(newQuery: String) {
        _query.value = newQuery
        searchDebounceJob?.cancel()
        searchDebounceJob = viewModelScope.launch {
            delay(500L)
            if (newQuery.isNotBlank()) {
                performSearch()
            }
        }
    }

    fun selectLanguage(lang: String?) {
        _selectedLanguage.value = if (_selectedLanguage.value == lang) null else lang
        performSearch()
    }

    fun setPushedDays(days: Int) {
        _pushedDays.value = days
        performSearch()
    }

    fun setNoForks(enabled: Boolean) {
        _noForks.value = enabled
        performSearch()
    }

    fun recordSearch(q: String) {
        val trimmed = q.trim()
        if (trimmed.isNotBlank()) {
            viewModelScope.launch {
                searchHistoryDao.insertQuery(SearchHistoryEntity(query = trimmed))
            }
        }
    }

    fun deleteRecentQuery(q: String) {
        viewModelScope.launch {
            searchHistoryDao.deleteQuery(q)
        }
    }

    fun clearRecentQueries() {
        viewModelScope.launch {
            searchHistoryDao.clearHistory()
        }
    }

    fun performSearch() {
        val q = _query.value.trim()
        if (q.isNotBlank()) {
            recordSearch(q)
        }
        if (q.isBlank() && _selectedLanguage.value == null) {
            _results.value = emptyList()
            return
        }

        searchDebounceJob?.cancel()
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            currentPage = 1
            _endReached.value = false

            val constructedPreset = Preset(
                title = "Поиск",
                queryKeywords = q,
                languages = listOfNotNull(_selectedLanguage.value),
                minStars = _minStars.value,
                pushedWithinDays = _pushedDays.value,
                excludeForks = _noForks.value
            )

            val apiQuery = constructedPreset.toGitHubQuery()
            val result = gitHubRepository.searchRepositories(
                query = apiQuery,
                sort = "stars",
                order = "desc",
                page = 1,
                perPage = 30
            )

            result.onSuccess { list ->
                if (list.isEmpty() || list.size < 30) {
                    _endReached.value = true
                }
                val enriched = list.map { repo ->
                    val isBookmarked = bookmarkRepository.isBookmarked(repo.id)
                    repo.copy(isBookmarked = isBookmarked)
                }
                _results.value = enriched
                _isLoading.value = false
            }.onFailure { e ->
                _isLoading.value = false
                _errorMessage.value = e.localizedMessage ?: "Ошибка поиска"
            }
        }
    }

    fun loadNextPage() {
        val q = _query.value.trim()
        if (q.isBlank() && _selectedLanguage.value == null) return
        if (_isLoading.value || _isLoadingMore.value || _endReached.value) return

        viewModelScope.launch {
            _isLoadingMore.value = true
            val nextPage = currentPage + 1

            val constructedPreset = Preset(
                title = "Поиск",
                queryKeywords = q,
                languages = listOfNotNull(_selectedLanguage.value),
                minStars = _minStars.value,
                pushedWithinDays = _pushedDays.value,
                excludeForks = _noForks.value
            )

            val apiQuery = constructedPreset.toGitHubQuery()
            val result = gitHubRepository.searchRepositories(
                query = apiQuery,
                sort = "stars",
                order = "desc",
                page = nextPage,
                perPage = 30
            )

            result.onSuccess { list ->
                if (list.isEmpty() || list.size < 30) {
                    _endReached.value = true
                }
                val enriched = list.map { repo ->
                    val isBookmarked = bookmarkRepository.isBookmarked(repo.id)
                    repo.copy(isBookmarked = isBookmarked)
                }
                _results.value = _results.value + enriched
                currentPage = nextPage
                _isLoadingMore.value = false
            }.onFailure {
                _isLoadingMore.value = false
            }
        }
    }

    fun saveCurrentSearchAsPreset(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val preset = Preset(
                title = title.trim(),
                queryKeywords = _query.value.trim(),
                languages = listOfNotNull(_selectedLanguage.value),
                minStars = _minStars.value,
                pushedWithinDays = _pushedDays.value,
                excludeForks = _noForks.value
            )
            presetRepository.createPreset(preset)
        }
    }

    fun toggleStar(item: RepositoryItem) {
        val current = item.isStarred
        val target = !current
        updateResult(item.id) { it.copy(isStarred = target) }

        viewModelScope.launch {
            val res = if (target) {
                gitHubRepository.starRepository(item.ownerLogin, item.name)
            } else {
                gitHubRepository.unstarRepository(item.ownerLogin, item.name)
            }
            res.onFailure {
                updateResult(item.id) { it.copy(isStarred = current) }
            }
        }
    }

    fun toggleBookmark(item: RepositoryItem) {
        viewModelScope.launch {
            val isNowBookmarked = bookmarkRepository.toggleBookmark(item)
            updateResult(item.id) { it.copy(isBookmarked = isNowBookmarked) }
        }
    }

    private fun updateResult(repoId: Long, transform: (RepositoryItem) -> RepositoryItem) {
        _results.value = _results.value.map {
            if (it.id == repoId) transform(it) else it
        }
    }
}
