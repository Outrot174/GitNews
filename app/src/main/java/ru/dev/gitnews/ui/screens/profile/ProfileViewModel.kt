package ru.dev.gitnews.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.dev.gitnews.data.local.SecureTokenStorage
import ru.dev.gitnews.data.local.dao.TranslationCacheDao
import ru.dev.gitnews.domain.model.AuthProvider
import ru.dev.gitnews.domain.repository.AuthRepository
import ru.dev.gitnews.domain.repository.GitHubRepository
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import ru.dev.gitnews.data.remote.GitHubRateLimitState
import ru.dev.gitnews.data.remote.GitHubRateLimitTracker
import ru.dev.gitnews.domain.repository.PresetRepository
import ru.dev.gitnews.domain.repository.ReleaseTrackerRepository

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val gitHubRepository: GitHubRepository,
    private val tokenStorage: SecureTokenStorage,
    private val presetRepository: PresetRepository,
    private val releaseTrackerRepository: ReleaseTrackerRepository
) : ViewModel() {

    val liveRateLimit: StateFlow<GitHubRateLimitState?> = GitHubRateLimitTracker.rateLimit

    private val _isNotificationsEnabled = MutableStateFlow(releaseTrackerRepository.isNotificationsEnabled())
    val isNotificationsEnabled: StateFlow<Boolean> = _isNotificationsEnabled.asStateFlow()

    private val _authProvider = MutableStateFlow(tokenStorage.getAuthProvider())
    val authProvider: StateFlow<AuthProvider> = _authProvider.asStateFlow()

    private val _userLogin = MutableStateFlow(tokenStorage.getUserLogin() ?: "Пользователь")
    val userLogin: StateFlow<String> = _userLogin.asStateFlow()

    private val _userName = MutableStateFlow(tokenStorage.getUserName())
    val userName: StateFlow<String?> = _userName.asStateFlow()

    private val _userEmail = MutableStateFlow(tokenStorage.getUserEmail())
    val userEmail: StateFlow<String?> = _userEmail.asStateFlow()

    private val _userAvatar = MutableStateFlow(tokenStorage.getUserAvatar())
    val userAvatar: StateFlow<String?> = _userAvatar.asStateFlow()

    private val _linkedGitHubToken = MutableStateFlow(tokenStorage.getGitHubToken())
    val linkedGitHubToken: StateFlow<String?> = _linkedGitHubToken.asStateFlow()

    private val _patInput = MutableStateFlow("")
    val patInput: StateFlow<String> = _patInput.asStateFlow()

    private val _tokenMessage = MutableStateFlow<String?>(null)
    val tokenMessage: StateFlow<String?> = _tokenMessage.asStateFlow()

    private val _isLinking = MutableStateFlow(false)
    val isLinking: StateFlow<Boolean> = _isLinking.asStateFlow()

    private val _rateLimitRemaining = MutableStateFlow(60)
    val rateLimitRemaining: StateFlow<Int> = _rateLimitRemaining.asStateFlow()

    private val _rateLimitResetTime = MutableStateFlow("")
    val rateLimitResetTime: StateFlow<String> = _rateLimitResetTime.asStateFlow()

    init {
        loadProfileData()
    }

    fun loadProfileData() {
        _authProvider.value = tokenStorage.getAuthProvider()
        _userLogin.value = tokenStorage.getUserLogin() ?: "Пользователь"
        _userName.value = tokenStorage.getUserName()
        _userEmail.value = tokenStorage.getUserEmail()
        _userAvatar.value = tokenStorage.getUserAvatar()
        _linkedGitHubToken.value = tokenStorage.getGitHubToken()

        viewModelScope.launch {
            val rateLimitResult = gitHubRepository.getRateLimit()
            rateLimitResult.onSuccess { (remaining, resetEpoch) ->
                _rateLimitRemaining.value = remaining
                val time = Instant.ofEpochSecond(resetEpoch)
                    .atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("HH:mm"))
                _rateLimitResetTime.value = time
            }
        }
    }

    fun toggleReleaseNotifications(enabled: Boolean) {
        _isNotificationsEnabled.value = enabled
        releaseTrackerRepository.setNotificationsEnabled(enabled)
    }

    suspend fun exportPresetsJson(): String {
        return presetRepository.exportPresetsJson()
    }

    suspend fun importPresetsJson(jsonString: String): Result<Int> {
        return presetRepository.importPresetsJson(jsonString)
    }

    fun onPatInputChanged(value: String) {
        _patInput.value = value
        _tokenMessage.value = null
    }

    fun linkGitHubToken() {
        val token = _patInput.value.trim()
        if (token.isBlank()) {
            _tokenMessage.value = "Введите токен GitHub (PAT)"
            return
        }

        _isLinking.value = true
        _tokenMessage.value = null

        viewModelScope.launch {
            val result = authRepository.linkGitHubToken(token)
            _isLinking.value = false
            result.onSuccess {
                _linkedGitHubToken.value = token
                _patInput.value = ""
                _tokenMessage.value = "GitHub токен успешно привязан"
                loadProfileData()
            }.onFailure { e ->
                _tokenMessage.value = e.localizedMessage ?: "Ошибка привязки токена"
            }
        }
    }

    fun unlinkGitHubToken() {
        viewModelScope.launch {
            authRepository.unlinkGitHubToken()
            _linkedGitHubToken.value = null
            _tokenMessage.value = "GitHub токен отвязан"
            loadProfileData()
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }
}
