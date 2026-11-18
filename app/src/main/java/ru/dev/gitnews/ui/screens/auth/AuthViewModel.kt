package ru.dev.gitnews.ui.screens.auth

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.util.Patterns
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.dev.gitnews.domain.model.AuthState
import ru.dev.gitnews.domain.repository.AuthRepository

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    val authState: StateFlow<AuthState> = authRepository.authState

    private val _isPolling = MutableStateFlow(false)
    val isPolling: StateFlow<Boolean> = _isPolling.asStateFlow()

    private val _patInput = MutableStateFlow("")
    val patInput: StateFlow<String> = _patInput.asStateFlow()

    private val _emailInput = MutableStateFlow("")
    val emailInput: StateFlow<String> = _emailInput.asStateFlow()

    private val _passwordInput = MutableStateFlow("")
    val passwordInput: StateFlow<String> = _passwordInput.asStateFlow()

    private val _displayNameInput = MutableStateFlow("")
    val displayNameInput: StateFlow<String> = _displayNameInput.asStateFlow()

    private val _isRegisterMode = MutableStateFlow(false)
    val isRegisterMode: StateFlow<Boolean> = _isRegisterMode.asStateFlow()

    private val _isPasswordVisible = MutableStateFlow(false)
    val isPasswordVisible: StateFlow<Boolean> = _isPasswordVisible.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var pollingJob: Job? = null

    init {
        viewModelScope.launch {
            authRepository.checkExistingAuth()
        }
    }

    fun onEmailChanged(value: String) {
        _emailInput.value = value
        _errorMessage.value = null
    }

    fun onPasswordChanged(value: String) {
        _passwordInput.value = value
        _errorMessage.value = null
    }

    fun onDisplayNameChanged(value: String) {
        _displayNameInput.value = value
        _errorMessage.value = null
    }

    fun toggleRegisterMode() {
        _isRegisterMode.value = !_isRegisterMode.value
        _errorMessage.value = null
    }

    fun togglePasswordVisibility() {
        _isPasswordVisible.value = !_isPasswordVisible.value
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun onPatInputChanged(value: String) {
        _patInput.value = value
        _errorMessage.value = null
    }

    fun authenticateWithEmail() {
        val email = _emailInput.value.trim()
        val password = _passwordInput.value
        val displayName = _displayNameInput.value.trim()

        if (email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _errorMessage.value = "Введите корректный адрес электронной почты"
            return
        }

        if (password.length < 6) {
            _errorMessage.value = "Пароль должен содержать минимум 6 символов"
            return
        }

        if (_isRegisterMode.value && displayName.isBlank()) {
            _errorMessage.value = "Введите имя пользователя"
            return
        }

        _errorMessage.value = null
        _isLoading.value = true

        viewModelScope.launch {
            val result = if (_isRegisterMode.value) {
                authRepository.registerWithEmail(email, password, displayName)
            } else {
                authRepository.signInWithEmail(email, password)
            }
            _isLoading.value = false
            result.onFailure { e ->
                _errorMessage.value = e.localizedMessage ?: "Ошибка авторизации"
            }
        }
    }

    fun signInWithGoogleAccount(accountName: String) {
        if (accountName.isBlank()) return
        _errorMessage.value = null
        _isLoading.value = true

        viewModelScope.launch {
            val name = accountName.substringBefore("@").replaceFirstChar {
                if (it.isLowerCase()) it.titlecase() else it.toString()
            }
            val result = authRepository.signInWithGoogle(
                displayName = name,
                email = accountName
            )
            _isLoading.value = false
            result.onFailure { e ->
                _errorMessage.value = e.localizedMessage ?: "Ошибка входа через Google"
            }
        }
    }

    fun startDeviceFlow(onCodeReady: ((userCode: String, verificationUri: String) -> Unit)? = null) {
        pollingJob?.cancel()
        _isPolling.value = false
        _errorMessage.value = null

        viewModelScope.launch {
            val result = authRepository.startDeviceFlow()
            result.onSuccess { authenticating ->
                _isPolling.value = true
                onCodeReady?.invoke(authenticating.userCode, authenticating.verificationUri)
                pollingJob = viewModelScope.launch {
                    val pollResult = authRepository.pollDeviceFlow(
                        deviceCode = authenticating.deviceCode,
                        intervalSeconds = authenticating.intervalSeconds
                    )
                    _isPolling.value = false
                    pollResult.onFailure { e ->
                        _errorMessage.value = e.localizedMessage
                    }
                }
            }.onFailure { e ->
                _errorMessage.value = e.localizedMessage
            }
        }
    }

    fun authenticateWithPat() {
        val token = _patInput.value.trim()
        if (token.isBlank()) {
            _errorMessage.value = "Введите Personal Access Token"
            return
        }

        _errorMessage.value = null
        viewModelScope.launch {
            val result = authRepository.authenticateWithToken(token)
            result.onFailure { e ->
                _errorMessage.value = e.localizedMessage
            }
        }
    }

    fun cancelDeviceFlow() {
        pollingJob?.cancel()
        _isPolling.value = false
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}
