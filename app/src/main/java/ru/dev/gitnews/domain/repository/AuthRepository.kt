package ru.dev.gitnews.domain.repository

import kotlinx.coroutines.flow.StateFlow
import ru.dev.gitnews.domain.model.AuthState

interface AuthRepository {
    val authState: StateFlow<AuthState>
    suspend fun checkExistingAuth()
    suspend fun startDeviceFlow(): Result<AuthState.Authenticating>
    suspend fun pollDeviceFlow(deviceCode: String, intervalSeconds: Int): Result<AuthState.Authenticated>
    suspend fun authenticateWithToken(token: String): Result<AuthState.Authenticated>
    suspend fun signInWithGoogle(displayName: String?, email: String?, photoUrl: String? = null): Result<Unit>
    suspend fun signInWithGoogleCredential(idToken: String): Result<AuthState.Authenticated>
    suspend fun signInWithEmail(email: String, password: String): Result<Unit>
    suspend fun registerWithEmail(email: String, password: String, displayName: String? = null): Result<Unit>
    suspend fun sendPasswordReset(email: String): Result<Unit>
    suspend fun linkGitHubToken(token: String): Result<Unit>
    suspend fun unlinkGitHubToken(): Result<Unit>
    fun getLinkedGitHubToken(): String?
    suspend fun logout()
    fun getCurrentToken(): String?
    fun isGoogleClientIdConfigured(): Boolean = false
    fun getGoogleWebClientId(): String? = null
}
