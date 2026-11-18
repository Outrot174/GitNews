package ru.dev.gitnews.domain.model

enum class AuthProvider(val title: String) {
    GOOGLE("Google"),
    EMAIL("Email"),
    GITHUB("GitHub")
}

sealed interface AuthState {
    data object Loading : AuthState
    data object Unauthenticated : AuthState

    data class Authenticating(
        val deviceCode: String,
        val userCode: String,
        val verificationUri: String,
        val intervalSeconds: Int
    ) : AuthState

    data class Authenticated(
        val provider: AuthProvider = AuthProvider.GITHUB,
        val login: String,
        val name: String? = null,
        val email: String? = null,
        val avatarUrl: String? = null,
        val token: String? = null,
        val githubToken: String? = null
    ) : AuthState

    data class Error(val message: String) : AuthState
}
