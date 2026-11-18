package ru.dev.gitnews.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import ru.dev.gitnews.data.local.SecureTokenStorage
import ru.dev.gitnews.data.remote.GitHubApiService
import ru.dev.gitnews.domain.model.AuthProvider
import ru.dev.gitnews.domain.model.AuthState
import ru.dev.gitnews.domain.repository.AuthRepository

class AuthRepositoryImpl(
    private val context: Context,
    private val apiService: GitHubApiService,
    private val tokenStorage: SecureTokenStorage
) : AuthRepository {

    private val firebaseAuth: FirebaseAuth? by lazy {
        try {
            val app = if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            } else {
                FirebaseApp.getInstance()
            }
            app?.let { FirebaseAuth.getInstance(it) }
        } catch (e: Exception) {
            Log.w("AuthRepositoryImpl", "FirebaseAuth is not available: ${e.message}")
            null
        }
    }

    private val _authState: MutableStateFlow<AuthState> = MutableStateFlow(
        run {
            val currentFbUser = try {
                if (FirebaseApp.getApps(context).isNotEmpty()) {
                    FirebaseAuth.getInstance().currentUser
                } else null
            } catch (_: Exception) {
                null
            }

            if (currentFbUser != null) {
                val provider = determineFirebaseProvider(currentFbUser)
                val email = currentFbUser.email
                val login = email?.substringBefore("@")
                    ?: currentFbUser.displayName?.replace(" ", "")
                    ?: "User"
                val name = currentFbUser.displayName ?: login
                val avatarUrl = currentFbUser.photoUrl?.toString()
                val token = currentFbUser.uid
                val githubToken = tokenStorage.getGitHubToken()

                tokenStorage.saveSession(
                    provider = provider,
                    login = login,
                    name = name,
                    email = email,
                    avatarUrl = avatarUrl,
                    token = token,
                    githubToken = githubToken
                )

                AuthState.Authenticated(
                    provider = provider,
                    login = login,
                    name = name,
                    email = email,
                    avatarUrl = avatarUrl,
                    token = token,
                    githubToken = githubToken
                )
            } else {
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
        }
    )
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        try {
            firebaseAuth?.addAuthStateListener { auth ->
                val fbUser = auth.currentUser
                if (fbUser != null) {
                    val provider = determineFirebaseProvider(fbUser)
                    val email = fbUser.email
                    val login = email?.substringBefore("@")
                        ?: fbUser.displayName?.replace(" ", "")
                        ?: "User"
                    val name = fbUser.displayName ?: login
                    val avatarUrl = fbUser.photoUrl?.toString()
                    val token = fbUser.uid
                    val githubToken = tokenStorage.getGitHubToken()

                    tokenStorage.saveSession(
                        provider = provider,
                        login = login,
                        name = name,
                        email = email,
                        avatarUrl = avatarUrl,
                        token = token,
                        githubToken = githubToken
                    )

                    _authState.value = AuthState.Authenticated(
                        provider = provider,
                        login = login,
                        name = name,
                        email = email,
                        avatarUrl = avatarUrl,
                        token = token,
                        githubToken = githubToken
                    )
                }
            }
        } catch (e: Exception) {
            Log.w("AuthRepositoryImpl", "Could not attach AuthStateListener: ${e.message}")
        }
    }

    // Configurable GitHub OAuth Client ID from BuildConfig (defaults to open-source fallback)
    private val defaultClientId = ru.dev.gitnews.BuildConfig.GITHUB_CLIENT_ID

    override suspend fun checkExistingAuth() {
        val currentFbUser = try {
            firebaseAuth?.currentUser
        } catch (_: Exception) {
            null
        }

        if (currentFbUser != null) {
            val provider = determineFirebaseProvider(currentFbUser)
            val email = currentFbUser.email
            val login = email?.substringBefore("@")
                ?: currentFbUser.displayName?.replace(" ", "")
                ?: "User"
            val name = currentFbUser.displayName ?: login
            val avatarUrl = currentFbUser.photoUrl?.toString()
            val token = currentFbUser.uid
            val githubToken = tokenStorage.getGitHubToken()

            tokenStorage.saveSession(
                provider = provider,
                login = login,
                name = name,
                email = email,
                avatarUrl = avatarUrl,
                token = token,
                githubToken = githubToken
            )

            _authState.value = AuthState.Authenticated(
                provider = provider,
                login = login,
                name = name,
                email = email,
                avatarUrl = avatarUrl,
                token = token,
                githubToken = githubToken
            )
            return
        }

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
                // If Firebase is available and currentUser is null, session is no longer active
                if (firebaseAuth != null) {
                    tokenStorage.clearToken()
                    _authState.value = AuthState.Unauthenticated
                } else {
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

    override suspend fun signInWithGoogleCredential(idToken: String): Result<AuthState.Authenticated> {
        val auth = firebaseAuth ?: return Result.failure(
            IllegalStateException("Firebase не инициализирован. Проверьте конфигурацию google-services.json.")
        )
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user ?: throw IllegalStateException("Не удалось получить профиль пользователя Google")

            val email = user.email
            val login = email?.substringBefore("@")
                ?: user.displayName?.replace(" ", "")
                ?: "GoogleUser"
            val name = user.displayName ?: login
            val avatarUrl = user.photoUrl?.toString()
            val githubToken = tokenStorage.getGitHubToken()

            tokenStorage.saveSession(
                provider = AuthProvider.GOOGLE,
                login = login,
                name = name,
                email = email,
                avatarUrl = avatarUrl,
                token = user.uid,
                githubToken = githubToken
            )

            val authenticatedState = AuthState.Authenticated(
                provider = AuthProvider.GOOGLE,
                login = login,
                name = name,
                email = email,
                avatarUrl = avatarUrl,
                token = user.uid,
                githubToken = githubToken
            )
            _authState.value = authenticatedState
            Result.success(authenticatedState)
        } catch (e: Throwable) {
            val mappedMessage = mapFirebaseAuthError(e)
            _authState.value = AuthState.Error(mappedMessage)
            Result.failure(Exception(mappedMessage, e))
        }
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
        val cleanEmail = email.trim()
        val auth = firebaseAuth

        if (auth != null) {
            return try {
                val authResult = auth.signInWithEmailAndPassword(cleanEmail, password).await()
                val user = authResult.user ?: throw IllegalStateException("Пользователь не найден")

                val login = cleanEmail.substringBefore("@")
                val name = user.displayName ?: login
                val avatarUrl = user.photoUrl?.toString()
                val githubToken = tokenStorage.getGitHubToken()

                tokenStorage.saveSession(
                    provider = AuthProvider.EMAIL,
                    login = login,
                    name = name,
                    email = cleanEmail,
                    avatarUrl = avatarUrl,
                    token = user.uid,
                    githubToken = githubToken
                )

                _authState.value = AuthState.Authenticated(
                    provider = AuthProvider.EMAIL,
                    login = login,
                    name = name,
                    email = cleanEmail,
                    avatarUrl = avatarUrl,
                    token = user.uid,
                    githubToken = githubToken
                )
                Result.success(Unit)
            } catch (e: Throwable) {
                val mappedMessage = mapFirebaseAuthError(e)
                _authState.value = AuthState.Error(mappedMessage)
                Result.failure(Exception(mappedMessage, e))
            }
        }

        // Fallback to local secure token storage if Firebase is absent
        val result = tokenStorage.loginEmailUser(cleanEmail.lowercase(), password)
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

    override suspend fun registerWithEmail(
        email: String,
        password: String,
        displayName: String?
    ): Result<Unit> {
        val cleanEmail = email.trim()
        val cleanName = displayName?.trim()?.ifBlank { null }
        val auth = firebaseAuth

        if (auth != null) {
            return try {
                val authResult = auth.createUserWithEmailAndPassword(cleanEmail, password).await()
                val user = authResult.user ?: throw IllegalStateException("Не удалось создать аккаунт")

                if (!cleanName.isNullOrBlank()) {
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(cleanName)
                        .build()
                    user.updateProfile(profileUpdates).await()
                }

                val login = cleanEmail.substringBefore("@")
                val name = cleanName ?: login
                val githubToken = tokenStorage.getGitHubToken()

                tokenStorage.saveSession(
                    provider = AuthProvider.EMAIL,
                    login = login,
                    name = name,
                    email = cleanEmail,
                    avatarUrl = null,
                    token = user.uid,
                    githubToken = githubToken
                )

                _authState.value = AuthState.Authenticated(
                    provider = AuthProvider.EMAIL,
                    login = login,
                    name = name,
                    email = cleanEmail,
                    avatarUrl = null,
                    token = user.uid,
                    githubToken = githubToken
                )
                Result.success(Unit)
            } catch (e: Throwable) {
                val mappedMessage = mapFirebaseAuthError(e)
                _authState.value = AuthState.Error(mappedMessage)
                Result.failure(Exception(mappedMessage, e))
            }
        }

        // Fallback to local secure token storage if Firebase is absent
        val login = cleanEmail.substringBefore("@")
        val name = cleanName ?: login
        val result = tokenStorage.registerEmailUser(cleanEmail.lowercase(), password, name)
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

    override suspend fun sendPasswordReset(email: String): Result<Unit> {
        val cleanEmail = email.trim()
        val auth = firebaseAuth ?: return Result.failure(
            IllegalStateException("Firebase не инициализирован. Проверьте конфигурацию google-services.json.")
        )
        return try {
            auth.sendPasswordResetEmail(cleanEmail).await()
            Result.success(Unit)
        } catch (e: Throwable) {
            val mappedMessage = mapFirebaseAuthError(e)
            Result.failure(Exception(mappedMessage, e))
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
        try {
            firebaseAuth?.signOut()
        } catch (e: Exception) {
            Log.w("AuthRepositoryImpl", "Firebase signOut: ${e.message}")
        }
        try {
            val credentialManager = androidx.credentials.CredentialManager.create(context)
            credentialManager.clearCredentialState(androidx.credentials.ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.w("AuthRepositoryImpl", "Clear credential state: ${e.message}")
        }
        tokenStorage.clearToken()
        _authState.value = AuthState.Unauthenticated
    }

    override fun getCurrentToken(): String? = tokenStorage.getToken()

    override fun getGoogleWebClientId(): String? {
        val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (resId != 0) {
            val resValue = context.getString(resId)
            if (resValue.isNotBlank()) return resValue
        }
        val id = ru.dev.gitnews.BuildConfig.GOOGLE_WEB_CLIENT_ID
        if (id.isNotBlank() &&
            !id.contains("placeholder", ignoreCase = true) &&
            !id.contains("YOUR_", ignoreCase = true)
        ) {
            return id
        }
        return null
    }

    override fun isGoogleClientIdConfigured(): Boolean {
        return !getGoogleWebClientId().isNullOrBlank()
    }

    private fun determineFirebaseProvider(user: FirebaseUser): AuthProvider {
        for (profile in user.providerData) {
            when (profile.providerId) {
                GoogleAuthProvider.PROVIDER_ID -> return AuthProvider.GOOGLE
                EmailAuthProvider.PROVIDER_ID -> return AuthProvider.EMAIL
            }
        }
        return if (user.email != null) AuthProvider.EMAIL else AuthProvider.GOOGLE
    }

    private fun mapFirebaseAuthError(e: Throwable): String {
        return when (e) {
            is FirebaseAuthInvalidUserException -> {
                "Пользователь с таким адресом электронной почты не найден"
            }
            is FirebaseAuthInvalidCredentialsException -> {
                "Неверный пароль или некорректный формат адреса почты"
            }
            is FirebaseAuthUserCollisionException -> {
                "Пользователь с таким адресом электронной почты уже зарегистрирован"
            }
            is FirebaseAuthWeakPasswordException -> {
                "Пароль слишком простой. Используйте минимум 6 символов"
            }
            is FirebaseNetworkException -> {
                "Ошибка сети. Проверьте интернет-соединение"
            }
            is FirebaseTooManyRequestsException -> {
                "Слишком много неудачных попыток. Пожалуйста, подождите некоторое время"
            }
            else -> {
                val msg = e.localizedMessage ?: e.message ?: "Ошибка авторизации"
                when {
                    msg.contains("user-not-found", ignoreCase = true) ->
                        "Пользователь с таким адресом не найден"
                    msg.contains("wrong-password", ignoreCase = true) ->
                        "Неверный пароль"
                    msg.contains("email-already-in-use", ignoreCase = true) ->
                        "Этот адрес электронной почты уже используется"
                    msg.contains("network", ignoreCase = true) ->
                        "Ошибка сети. Проверьте интернет-соединение"
                    msg.contains("weak-password", ignoreCase = true) ->
                        "Пароль должен содержать минимум 6 символов"
                    msg.contains("invalid-email", ignoreCase = true) ->
                        "Некорректный формат адреса электронной почты"
                    else -> msg
                }
            }
        }
    }
}
