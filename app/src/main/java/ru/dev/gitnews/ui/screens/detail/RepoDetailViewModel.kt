package ru.dev.gitnews.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.dev.gitnews.domain.model.ReleaseItem
import ru.dev.gitnews.domain.model.RepositoryDetail
import ru.dev.gitnews.domain.repository.BookmarkRepository
import ru.dev.gitnews.domain.repository.GitHubRepository
import ru.dev.gitnews.ml.MlKitTranslationManager

data class RepoDetailUiState(
    val isReadmeLoading: Boolean = true,
    val readmeContent: String? = null,
    val readmeError: String? = null
)

class RepoDetailViewModel(
    rawOwner: String,
    rawRepo: String,
    private val gitHubRepository: GitHubRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val translationManager: MlKitTranslationManager
) : ViewModel() {

    val owner: String = try {
        java.net.URLDecoder.decode(rawOwner, java.nio.charset.StandardCharsets.UTF_8.name())
    } catch (_: Exception) { rawOwner }

    val repo: String = try {
        java.net.URLDecoder.decode(rawRepo, java.nio.charset.StandardCharsets.UTF_8.name())
    } catch (_: Exception) { rawRepo }

    private val _detail = MutableStateFlow<RepositoryDetail?>(null)
    val detail: StateFlow<RepositoryDetail?> = _detail.asStateFlow()

    private val _releases = MutableStateFlow<List<ReleaseItem>>(emptyList())
    val releases: StateFlow<List<ReleaseItem>> = _releases.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isReadmeLoading = MutableStateFlow(true)
    val isReadmeLoading: StateFlow<Boolean> = _isReadmeLoading.asStateFlow()

    private val _readmeContent = MutableStateFlow<String?>(null)
    val readmeContent: StateFlow<String?> = _readmeContent.asStateFlow()

    private val _readmeError = MutableStateFlow<String?>(null)
    val readmeError: StateFlow<String?> = _readmeError.asStateFlow()

    val uiState: StateFlow<RepoDetailUiState> = combine(
        _isReadmeLoading,
        _readmeContent,
        _readmeError
    ) { loading, content, error ->
        RepoDetailUiState(
            isReadmeLoading = loading,
            readmeContent = content,
            readmeError = error
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, RepoDetailUiState(isReadmeLoading = true))

    private val _isReadmeTranslated = MutableStateFlow(false)
    val isReadmeTranslated: StateFlow<Boolean> = _isReadmeTranslated.asStateFlow()

    private val _isTranslating = MutableStateFlow(false)
    val isTranslating: StateFlow<Boolean> = _isTranslating.asStateFlow()

    private val _translatedReadme = MutableStateFlow<String?>(null)
    val translatedReadme: StateFlow<String?> = _translatedReadme.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            _isReadmeLoading.value = true
            _readmeError.value = null
            _errorMessage.value = null

            runCatching {
                gitHubRepository.getRepositoryDetail(owner, repo)
            }.onSuccess { detailResult ->
                detailResult.onSuccess { d ->
                    _detail.value = d
                    _isLoading.value = false

                    val readme = d.rawReadmeContent
                    if (!readme.isNullOrBlank()) {
                        _readmeContent.value = readme
                        _isReadmeLoading.value = false
                    } else {
                        // Direct fallback attempt for README
                        val directResult = runCatching {
                            gitHubRepository.getRawReadme(owner, repo)
                        }.getOrNull()

                        val directReadme = directResult?.getOrNull()
                        if (!directReadme.isNullOrBlank()) {
                            _readmeContent.value = directReadme
                            _detail.value = d.copy(rawReadmeContent = directReadme)
                        } else {
                            _readmeContent.value = null
                            if (directResult?.isFailure == true) {
                                _readmeError.value = directResult.exceptionOrNull()?.localizedMessage
                            }
                        }
                        _isReadmeLoading.value = false
                    }
                }.onFailure { e ->
                    _isLoading.value = false
                    _isReadmeLoading.value = false
                    _readmeError.value = e.localizedMessage
                    _errorMessage.value = e.localizedMessage ?: "Ошибка загрузки репозитория"
                }
            }.onFailure { e ->
                _isLoading.value = false
                _isReadmeLoading.value = false
                _readmeError.value = e.localizedMessage
                _errorMessage.value = e.localizedMessage ?: "Ошибка загрузки репозитория"
            }

            runCatching {
                gitHubRepository.getReleases(owner, repo, page = 1)
            }.onSuccess { releasesResult ->
                releasesResult.onSuccess { rList ->
                    _releases.value = rList
                }
            }
        }
    }

    fun retryReadme() {
        viewModelScope.launch {
            _isReadmeLoading.value = true
            _readmeError.value = null
            val result = gitHubRepository.getRawReadme(owner, repo)
            result.onSuccess { content ->
                if (content.isNotBlank()) {
                    _readmeContent.value = content
                    _readmeError.value = null
                    _detail.value = _detail.value?.copy(rawReadmeContent = content)
                } else {
                    _readmeError.value = "README.md пуст"
                }
            }.onFailure { e ->
                _readmeError.value = e.localizedMessage ?: "Не удалось загрузить README.md"
            }
            _isReadmeLoading.value = false
        }
    }

    fun toggleStar() {
        val currentDetail = _detail.value ?: return
        val item = currentDetail.item
        val targetStarred = !item.isStarred

        _detail.value = currentDetail.copy(
            item = item.copy(
                isStarred = targetStarred,
                starsCount = if (targetStarred) item.starsCount + 1 else (item.starsCount - 1).coerceAtLeast(0)
            )
        )

        viewModelScope.launch {
            val res = runCatching {
                if (targetStarred) {
                    gitHubRepository.starRepository(owner, repo)
                } else {
                    gitHubRepository.unstarRepository(owner, repo)
                }
            }.getOrElse { Result.failure(it) }

            res.onFailure {
                _detail.value = currentDetail
            }
        }
    }

    fun toggleBookmark() {
        val currentDetail = _detail.value ?: return
        val item = currentDetail.item

        viewModelScope.launch {
            val newBookmarked = bookmarkRepository.toggleBookmark(item)
            _detail.value = currentDetail.copy(
                item = item.copy(isBookmarked = newBookmarked)
            )
        }
    }

    fun toggleTranslateReadme() {
        val currentDetail = _detail.value
        val raw = _readmeContent.value ?: currentDetail?.rawReadmeContent ?: return
        if (raw.isBlank()) return

        if (_isReadmeTranslated.value) {
            _isReadmeTranslated.value = false
            return
        }

        if (_translatedReadme.value != null) {
            _isReadmeTranslated.value = true
            return
        }

        viewModelScope.launch {
            _isTranslating.value = true
            runCatching {
                translationManager.translateMarkdown(raw)
            }.onSuccess { translated ->
                _translatedReadme.value = translated
                _isReadmeTranslated.value = true
            }.onFailure {
                _isReadmeTranslated.value = false
            }
            _isTranslating.value = false
        }
    }
}
