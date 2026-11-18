package ru.dev.gitnews.domain.repository

import ru.dev.gitnews.domain.model.ReleaseItem
import ru.dev.gitnews.domain.model.RepositoryDetail
import ru.dev.gitnews.domain.model.RepositoryItem

interface GitHubRepository {
    suspend fun searchRepositories(
        query: String,
        sort: String = "stars",
        order: String = "desc",
        page: Int = 1,
        perPage: Int = 30
    ): Result<List<RepositoryItem>>

    suspend fun getRepositoryDetail(owner: String, repo: String): Result<RepositoryDetail>
    suspend fun getRawReadme(owner: String, repo: String): Result<String>
    suspend fun isRepoStarred(owner: String, repo: String): Result<Boolean>
    suspend fun starRepository(owner: String, repo: String): Result<Unit>
    suspend fun unstarRepository(owner: String, repo: String): Result<Unit>
    suspend fun getReleases(owner: String, repo: String, page: Int = 1): Result<List<ReleaseItem>>
    suspend fun getRateLimit(): Result<Pair<Int, Long>>
}
