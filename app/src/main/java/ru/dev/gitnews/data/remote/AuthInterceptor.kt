package ru.dev.gitnews.data.remote

import okhttp3.Interceptor
import okhttp3.Response
import ru.dev.gitnews.data.local.SecureTokenStorage

/**
 * Injects GitHub Bearer Token securely from EncryptedSharedPreferences
 * and captures live X-RateLimit response headers.
 */
class AuthInterceptor(
    private val tokenStorage: SecureTokenStorage
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val effectiveToken = tokenStorage.getGitHubToken() ?: tokenStorage.getToken()
        val builder = originalRequest.newBuilder()
        if (!effectiveToken.isNullOrBlank() &&
            !effectiveToken.startsWith("google:") &&
            !effectiveToken.startsWith("email:") &&
            !effectiveToken.startsWith("session:") &&
            originalRequest.url.host.contains("github.com")) {
            builder.header("Authorization", "Bearer $effectiveToken")
        }
        if (originalRequest.header("Accept").isNullOrBlank()) {
            builder.header("Accept", "application/vnd.github.v3+json")
        }

        val response = chain.proceed(builder.build())

        // Capture live GitHub rate limits
        val remaining = response.header("X-RateLimit-Remaining")?.toIntOrNull()
        val limit = response.header("X-RateLimit-Limit")?.toIntOrNull()
        val reset = response.header("X-RateLimit-Reset")?.toLongOrNull() ?: 0L
        if (remaining != null && limit != null) {
            GitHubRateLimitTracker.update(remaining, limit, reset)
        }

        return response
    }
}
