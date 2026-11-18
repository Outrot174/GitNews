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

    private val _resetMessage = MutableStateFlow<String?>(null)
    val resetMessage: StateFlow<String?> = _resetMessage.asStateFlow()

    private var pollingJob: Job? = null

    init {
        viewModelScope.launch {
            authRepository.checkExistingAuth()
        }
    }

    fun onEmailChanged(value: String) {
        _emailInput.value = value
        _errorMessage.value = null
        _resetMessage.value = null
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
        _resetMessage.value = null
    }

    fun togglePasswordVisibility() {
        _isPasswordVisible.value = !_isPasswordVisible.value
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearResetMessage() {
        _resetMessage.value = null
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
        _resetMessage.value = null
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

    fun sendPasswordReset() {
        val email = _emailInput.value.trim()
        if (email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _errorMessage.value = "Введите адрес электронной почты для сброса пароля"
            return
        }

        _errorMessage.value = null
        _resetMessage.value = null
        _isLoading.value = true

        viewModelScope.launch {
            val result = authRepository.sendPasswordReset(email)
            _isLoading.value = false
            result.onSuccess {
                _resetMessage.value = "Письмо для сброса пароля отправлено на $email"
            }.onFailure { e ->
                _errorMessage.value = e.localizedMessage ?: "Не удалось отправить письмо для сброса пароля"
            }
        }
    }

    fun signInWithGoogle(context: Context) {
        val webClientId = authRepository.getGoogleWebClientId()
        if (webClientId.isNullOrBlank()) {
            _errorMessage.value = "Google Client ID не настроен. Добавьте google-services.json или укажите GOOGLE_WEB_CLIENT_ID"
            return
        }

        _errorMessage.value = null
        _resetMessage.value = null
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val credentialManager = androidx.credentials.CredentialManager.create(context)
                val googleIdOption = com.google.android.libraries.identity.googleid.GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = androidx.credentials.GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val response = credentialManager.getCredential(context, request)
                val credential = response.credential

                if (credential is androidx.credentials.CustomCredential &&
                    credential.type == com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleIdTokenCredential = com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken
                    val result = authRepository.signInWithGoogleCredential(idToken)
                    _isLoading.value = false
                    result.onFailure { e ->
                        _errorMessage.value = e.localizedMessage ?: "Ошибка авторизации через Google"
                    }
                } else {
                    _isLoading.value = false
                    _errorMessage.value = "Не удалось получить учетные данные Google"
                }
            } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
                _isLoading.value = false
            } catch (e: androidx.credentials.exceptions.NoCredentialException) {
                _isLoading.value = false
                _errorMessage.value = "На устройстве не найдены аккаунты Google"
            } catch (e: androidx.credentials.exceptions.GetCredentialException) {
                _isLoading.value = false
                _errorMessage.value = e.localizedMessage ?: "Ошибка выбора аккаунта Google"
            } catch (e: Throwable) {
                _isLoading.value = false
                _errorMessage.value = e.localizedMessage ?: "Ошибка входа через Google"
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
