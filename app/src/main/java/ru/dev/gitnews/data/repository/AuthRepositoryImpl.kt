package ru.dev.gitnews.data.repository

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.dev.gitnews.data.local.SecureTokenStorage
import ru.dev.gitnews.data.remote.GitHubApiService
import ru.dev.gitnews.domain.model.AuthProvider
import ru.dev.gitnews.domain.model.AuthState
import ru.dev.gitnews.domain.repository.AuthRepository

class AuthRepositoryImpl(
    private val apiService: GitHubApiService,
    private val tokenStorage: SecureTokenStorage
) : AuthRepository {

    private val _authState: MutableStateFlow<AuthState> = MutableStateFlow(
        run {
            val token = tokenStorage.getToken()
            if (!token.isNullOrBlank()) {
                val provider = tokenStorage.getAuthProvider()
                val cachedLogin = tokenStorage.getUserLogin() ?: "User"
                val email = tokenStorage.getUserEmail()
                val githubToken = tokenStorage.getGitHubToken() ?: (if (provider == AuthProvider.GITHUB) token else null)
                AuthState.Authenticated(
                    provider = provider,
                    login = cachedLogin,
                    name = tokenStorage.getUserName() ?: cachedLogin,
                    email = email,
                    avatarUrl = tokenStorage.getUserAvatar(),
                    token = token,
                    githubToken = githubToken
                )
            } else {
                AuthState.Loading
            }
        }
    )
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // Configurable GitHub OAuth Client ID from BuildConfig (defaults to open-source fallback)
    private val defaultClientId = ru.dev.gitnews.BuildConfig.GITHUB_CLIENT_ID

    override suspend fun checkExistingAuth() {
        val token = tokenStorage.getToken()
        if (token.isNullOrBlank()) {
            _authState.value = AuthState.Unauthenticated
            return
        }

        val provider = tokenStorage.getAuthProvider()
        val email = tokenStorage.getUserEmail()
        val cachedLogin = tokenStorage.getUserLogin() ?: email?.substringBefore("@") ?: "User"
        val cachedName = tokenStorage.getUserName() ?: cachedLogin
        val cachedAvatar = tokenStorage.getUserAvatar()
        val githubToken = tokenStorage.getGitHubToken() ?: (if (provider == AuthProvider.GITHUB) token else null)

        when (provider) {
            AuthProvider.GOOGLE, AuthProvider.EMAIL -> {
                _authState.value = AuthState.Authenticated(
                    provider = provider,
                    login = cachedLogin,
                    name = cachedName,
                    email = email,
                    avatarUrl = cachedAvatar,
                    token = token,
                    githubToken = githubToken
                )
            }
            AuthProvider.GITHUB -> {
                val user = runCatching { apiService.getCurrentUser() }.getOrNull()
                if (user != null) {
                    tokenStorage.saveUser(user.login, user.name, user.avatarUrl)
                    _authState.value = AuthState.Authenticated(
                        provider = AuthProvider.GITHUB,
                        login = user.login,
                        name = user.name ?: user.login,
                        email = user.email ?: email,
                        avatarUrl = user.avatarUrl,
                        token = token,
                        githubToken = token
                    )
                } else {
                    _authState.value = AuthState.Authenticated(
                        provider = AuthProvider.GITHUB,
                        login = cachedLogin,
                        name = cachedName,
                        email = email,
                        avatarUrl = cachedAvatar,
                        token = token,
                        githubToken = token
                    )
                }
            }
        }
    }

    override suspend fun startDeviceFlow(): Result<AuthState.Authenticating> {
        if (defaultClientId.isBlank()) {
            val msg = "GITHUB_CLIENT_ID не настроен. Для входа используйте Google, Email или укажите токен (PAT) в профиле."
            _authState.value = AuthState.Error(msg)
            return Result.failure(IllegalStateException(msg))
        }
        return try {
            val response = apiService.requestDeviceCode(clientId = defaultClientId)
            val state = AuthState.Authenticating(
                deviceCode = response.deviceCode,
                userCode = response.userCode,
                verificationUri = response.verificationUri,
                intervalSeconds = response.interval.coerceAtLeast(5)
            )
            _authState.value = state
            Result.success(state)
        } catch (e: Exception) {
            val error = "Ошибка запроса кода устройства: ${e.localizedMessage}"
            _authState.value = AuthState.Error(error)
            Result.failure(e)
        }
    }

    override suspend fun pollDeviceFlow(
        deviceCode: String,
        intervalSeconds: Int
    ): Result<AuthState.Authenticated> {
        var currentInterval = intervalSeconds.coerceAtLeast(5)
        val startTime = System.currentTimeMillis()
        val timeoutMs = 900_000L // 15 minutes timeout

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            delay(currentInterval * 1000L)
            try {
                val tokenResponse = apiService.pollDeviceToken(
                    clientId = defaultClientId,
                    deviceCode = deviceCode
                )

                if (!tokenResponse.accessToken.isNullOrBlank()) {
                    val token = tokenResponse.accessToken
                    tokenStorage.saveToken(token)

                    val user = runCatching { apiService.getCurrentUser() }.getOrNull()
                    val login = user?.login ?: "User"
                    val name = user?.name ?: login
                    val avatarUrl = user?.avatarUrl

                    tokenStorage.saveSession(
                        provider = AuthProvider.GITHUB,
                        login = login,
                        name = name,
                        email = user?.email,
                        avatarUrl = avatarUrl,
                        token = token,
                        githubToken = token
                    )

                    val authenticatedState = AuthState.Authenticated(
                        provider = AuthProvider.GITHUB,
                        login = login,
                        name = name,
                        email = user?.email,
                        avatarUrl = avatarUrl,
                        token = token,
                        githubToken = token
                    )
                    _authState.value = authenticatedState
                    return Result.success(authenticatedState)
                }

                when (tokenResponse.error) {
                    "authorization_pending" -> {
                        // Continue polling
                    }
                    "slow_down" -> {
                        currentInterval += 5
                    }
                    "expired_token" -> {
                        val err = "Срок действия кода авторизации истек. Запросите новый код."
                        _authState.value = AuthState.Error(err)
                        return Result.failure(IllegalStateException(err))
                    }
                    "access_denied" -> {
                        val err = "Авторизация отклонена пользователем."
                        _authState.value = AuthState.Error(err)
                        return Result.failure(IllegalStateException(err))
                    }
                    else -> {
                        if (tokenResponse.error != null) {
                            val err = tokenResponse.errorDescription ?: tokenResponse.error ?: "Ошибка авторизации"
                            _authState.value = AuthState.Error(err)
                            return Result.failure(IllegalStateException(err))
                        }
                    }
                }
            } catch (e: Exception) {
                // If it's a network glitch, wait and continue polling
                delay(2000L)
            }
        }

        val timeoutErr = "Время ожидания подтверждения истекло."
        _authState.value = AuthState.Error(timeoutErr)
        return Result.failure(IllegalStateException(timeoutErr))
    }

    override suspend fun authenticateWithToken(token: String): Result<AuthState.Authenticated> {
        val cleanToken = token.trim()
        if (cleanToken.isBlank()) {
            return Result.failure(IllegalArgumentException("Токен не может быть пустым"))
        }

        tokenStorage.saveToken(cleanToken)
        val user = runCatching { apiService.getCurrentUser() }.getOrNull()
        val login = user?.login ?: "User"
        val name = user?.name ?: login
        val avatarUrl = user?.avatarUrl

        tokenStorage.saveSession(
            provider = AuthProvider.GITHUB,
            login = login,
            name = name,
            email = user?.email,
            avatarUrl = avatarUrl,
            token = cleanToken,
            githubToken = cleanToken
        )

        val state = AuthState.Authenticated(
            provider = AuthProvider.GITHUB,
            login = login,
            name = name,
            email = user?.email,
            avatarUrl = avatarUrl,
            token = cleanToken,
            githubToken = cleanToken
        )
        _authState.value = state
        return Result.success(state)
    }

    override suspend fun signInWithGoogle(displayName: String?, email: String?, photoUrl: String?): Result<Unit> {
        val cleanEmail = email?.trim() ?: "google_user@gmail.com"
        val login = cleanEmail.substringBefore("@")
        val name = displayName ?: login
        val token = "google:$cleanEmail"

        tokenStorage.saveSession(
            provider = AuthProvider.GOOGLE,
            login = login,
            name = name,
            email = cleanEmail,
            avatarUrl = photoUrl,
            token = token,
            githubToken = tokenStorage.getGitHubToken()
        )

        _authState.value = AuthState.Authenticated(
            provider = AuthProvider.GOOGLE,
            login = login,
            name = name,
            email = cleanEmail,
            avatarUrl = photoUrl,
            token = token,
            githubToken = tokenStorage.getGitHubToken()
        )
        return Result.success(Unit)
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<Unit> {
        val cleanEmail = email.trim().lowercase()
        val result = tokenStorage.loginEmailUser(cleanEmail, password)
        return result.map { storedName ->
            val login = cleanEmail.substringBefore("@")
            val name = storedName ?: login
            val token = "email:$cleanEmail"

            tokenStorage.saveSession(
                provider = AuthProvider.EMAIL,
                login = login,
                name = name,
                email = cleanEmail,
                avatarUrl = null,
                token = token,
                githubToken = tokenStorage.getGitHubToken()
            )

            _authState.value = AuthState.Authenticated(
                provider = AuthProvider.EMAIL,
                login = login,
                name = name,
                email = cleanEmail,
                avatarUrl = null,
                token = token,
                githubToken = tokenStorage.getGitHubToken()
            )
        }
    }

    override suspend fun registerWithEmail(email: String, password: String, displayName: String?): Result<Unit> {
        val cleanEmail = email.trim().lowercase()
        val login = cleanEmail.substringBefore("@")
        val name = displayName?.trim()?.ifBlank { null } ?: login
        val result = tokenStorage.registerEmailUser(cleanEmail, password, name)
        return result.map {
            val token = "email:$cleanEmail"
            tokenStorage.saveSession(
                provider = AuthProvider.EMAIL,
                login = login,
                name = name,
                email = cleanEmail,
                avatarUrl = null,
                token = token,
                githubToken = tokenStorage.getGitHubToken()
            )

            _authState.value = AuthState.Authenticated(
                provider = AuthProvider.EMAIL,
                login = login,
                name = name,
                email = cleanEmail,
                avatarUrl = null,
                token = token,
                githubToken = tokenStorage.getGitHubToken()
            )
        }
    }

    override suspend fun linkGitHubToken(token: String): Result<Unit> {
        val clean = token.trim()
        if (clean.isBlank()) return Result.failure(IllegalArgumentException("Токен не может быть пустым"))
        tokenStorage.saveGitHubToken(clean)
        val current = _authState.value
        if (current is AuthState.Authenticated) {
            _authState.value = current.copy(githubToken = clean)
        }
        return Result.success(Unit)
    }

    override suspend fun unlinkGitHubToken(): Result<Unit> {
        tokenStorage.saveGitHubToken(null)
        val current = _authState.value
        if (current is AuthState.Authenticated) {
            _authState.value = current.copy(githubToken = null)
        }
        return Result.success(Unit)
    }

    override fun getLinkedGitHubToken(): String? = tokenStorage.getGitHubToken()

    override suspend fun logout() {
        tokenStorage.clearToken()
        _authState.value = AuthState.Unauthenticated
    }

    override fun getCurrentToken(): String? = tokenStorage.getToken()

    override fun isGoogleClientIdConfigured(): Boolean {
        val id = ru.dev.gitnews.BuildConfig.GOOGLE_WEB_CLIENT_ID
        return id.isNotBlank() &&
                !id.contains("placeholder", ignoreCase = true) &&
                !id.contains("YOUR_", ignoreCase = true)
    }
}
