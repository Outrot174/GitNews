package ru.dev.gitnews.data.remote

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class GitHubRateLimitState(
    val remaining: Int,
    val limit: Int,
    val resetEpochSeconds: Long = 0L
)

object GitHubRateLimitTracker {
    private val _rateLimit = MutableStateFlow<GitHubRateLimitState?>(null)
    val rateLimit: StateFlow<GitHubRateLimitState?> = _rateLimit.asStateFlow()

    fun update(remaining: Int, limit: Int, resetEpochSeconds: Long) {
        _rateLimit.value = GitHubRateLimitState(remaining, limit, resetEpochSeconds)
    }
}
