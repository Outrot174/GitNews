package ru.dev.gitnews.data.repository

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import ru.dev.gitnews.data.local.dao.BookmarkDao
import ru.dev.gitnews.data.remote.GitHubApiService
import ru.dev.gitnews.data.remote.dto.GitHubRepoDto
import ru.dev.gitnews.domain.model.ReleaseAsset
import ru.dev.gitnews.domain.model.ReleaseItem
import ru.dev.gitnews.domain.model.RepositoryDetail
import ru.dev.gitnews.domain.model.RepositoryItem
import ru.dev.gitnews.domain.repository.GitHubRepository
import java.nio.charset.StandardCharsets

class GitHubRepositoryImpl(
    private val apiService: GitHubApiService,
    private val bookmarkDao: BookmarkDao,
    private val okHttpClient: OkHttpClient = OkHttpClient()
) : GitHubRepository {

    override suspend fun searchRepositories(
        query: String,
        sort: String,
        order: String,
        page: Int,
        perPage: Int
    ): Result<List<RepositoryItem>> {
        return try {
            val response = apiService.searchRepositories(
                query = query,
                sort = sort,
                order = order,
                page = page,
                perPage = perPage
            )

            val items = response.items.map { dto ->
                val isBookmarked = bookmarkDao.isBookmarked(dto.id)
                mapDtoToDomain(dto, isStarred = false, isBookmarked = isBookmarked)
            }
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getRepositoryDetail(owner: String, repo: String): Result<RepositoryDetail> {
        return try {
            val dto = apiService.getRepository(owner, repo)
            val isBookmarked = bookmarkDao.isBookmarked(dto.id)

            var isStarred = false
            try {
                val starCheck = apiService.checkIsStarred(owner, repo)
                isStarred = starCheck.code() == 204
            } catch (_: Exception) {}

            var releases: List<ReleaseItem> = emptyList()
            try {
                releases = apiService.getReleases(owner, repo, page = 1, perPage = 10).map { rDto ->
                    ReleaseItem(
                        id = rDto.id,
                        tagName = rDto.tagName,
                        name = rDto.name ?: rDto.tagName,
                        body = rDto.body,
                        publishedAt = rDto.publishedAt,
                        htmlUrl = rDto.htmlUrl,
                        isPrerelease = rDto.prerelease,
                        assets = rDto.assets.map { aDto ->
                            ReleaseAsset(
                                id = aDto.id,
                                name = aDto.name,
                                sizeBytes = aDto.size,
                                downloadCount = aDto.downloadCount,
                                browserDownloadUrl = aDto.browserDownloadUrl
                            )
                        }
                    )
                }
            } catch (_: Exception) {}

            val readmeResult = fetchRawReadme(owner, repo, dto.defaultBranch)
            val rawReadme = readmeResult.getOrNull()

            val item = mapDtoToDomain(dto, isStarred = isStarred, isBookmarked = isBookmarked)
            val detail = RepositoryDetail(
                item = item,
                defaultBranch = dto.defaultBranch,
                openIssuesCount = dto.openIssuesCount,
                subscribersCount = dto.subscribersCount,
                rawReadmeContent = rawReadme,
                latestRelease = releases.firstOrNull(),
                totalReleasesCount = releases.size
            )
            Result.success(detail)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getRawReadme(owner: String, repo: String): Result<String> {
        return fetchRawReadme(owner, repo, defaultBranch = null)
    }

    private suspend fun fetchRawReadme(
        owner: String,
        repo: String,
        defaultBranch: String?
    ): Result<String> = withContext(Dispatchers.IO) {
        // Step 1: Direct OkHttp Request to GitHub API with Accept: application/vnd.github.raw+json
        try {
            val apiRequest = Request.Builder()
                .url("https://api.github.com/repos/$owner/$repo/readme")
                .header("Accept", "application/vnd.github.raw+json")
                .get()
                .build()

            okHttpClient.newCall(apiRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string()?.trim()
                    if (!bodyString.isNullOrBlank()) {
                        val extracted = extractRawMarkdown(bodyString)
                        if (extracted.isNotBlank()) {
                            return@withContext Result.success(extracted)
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // Step 2: Fallback to raw.githubusercontent.com HEAD/README.md
        try {
            val headRequest = Request.Builder()
                .url("https://raw.githubusercontent.com/$owner/$repo/HEAD/README.md")
                .get()
                .build()

            okHttpClient.newCall(headRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string()?.trim()
                    if (!bodyString.isNullOrBlank()) {
                        return@withContext Result.success(bodyString)
                    }
                }
            }
        } catch (_: Exception) {}

        // Step 3: Fallback using defaultBranch or master/main
        val candidateBranches = mutableListOf<String>()
        if (!defaultBranch.isNullOrBlank()) candidateBranches.add(defaultBranch)
        if (!candidateBranches.contains("main")) candidateBranches.add("main")
        if (!candidateBranches.contains("master")) candidateBranches.add("master")

        for (branch in candidateBranches) {
            for (filename in listOf("README.md", "readme.md", "README.markdown", "Readme.md")) {
                try {
                    val branchRequest = Request.Builder()
                        .url("https://raw.githubusercontent.com/$owner/$repo/$branch/$filename")
                        .get()
                        .build()

                    okHttpClient.newCall(branchRequest).execute().use { response ->
                        if (response.isSuccessful) {
                            val bodyString = response.body?.string()?.trim()
                            if (!bodyString.isNullOrBlank()) {
                                return@withContext Result.success(bodyString)
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        }

        Result.failure(IllegalStateException("README.md не найден или пуст для $owner/$repo"))
    }

    private fun extractRawMarkdown(rawString: String): String {
        val trimmed = rawString.trim()
        if (trimmed.startsWith("{") && trimmed.contains("\"content\"")) {
            try {
                val json = JSONObject(trimmed)
                if (json.has("content")) {
                    val encoded = json.getString("content").replace("\\s+".toRegex(), "")
                    val decodedBytes = Base64.decode(encoded, Base64.DEFAULT)
                    val decoded = String(decodedBytes, StandardCharsets.UTF_8).trim()
                    if (decoded.isNotBlank()) return decoded
                }
            } catch (_: Exception) {}
        }
        return trimmed
    }

    override suspend fun isRepoStarred(owner: String, repo: String): Result<Boolean> {
        return try {
            val response = apiService.checkIsStarred(owner, repo)
            Result.success(response.code() == 204)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun starRepository(owner: String, repo: String): Result<Unit> {
        return try {
            val response = apiService.starRepository(owner, repo)
            if (response.isSuccessful || response.code() == 204) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unstarRepository(owner: String, repo: String): Result<Unit> {
        return try {
            val response = apiService.unstarRepository(owner, repo)
            if (response.isSuccessful || response.code() == 204) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getReleases(owner: String, repo: String, page: Int): Result<List<ReleaseItem>> {
        return try {
            val dtos = apiService.getReleases(owner, repo, page = page, perPage = 30)
            val list = dtos.map { rDto ->
                ReleaseItem(
                    id = rDto.id,
                    tagName = rDto.tagName,
                    name = rDto.name ?: rDto.tagName,
                    body = rDto.body,
                    publishedAt = rDto.publishedAt,
                    htmlUrl = rDto.htmlUrl,
                    isPrerelease = rDto.prerelease,
                    assets = rDto.assets.map { aDto ->
                        ReleaseAsset(
                            id = aDto.id,
                            name = aDto.name,
                            sizeBytes = aDto.size,
                            downloadCount = aDto.downloadCount,
                            browserDownloadUrl = aDto.browserDownloadUrl
                        )
                    }
                )
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getRateLimit(): Result<Pair<Int, Long>> {
        return try {
            val response = apiService.getRateLimit()
            val remaining = response.resources.core.remaining
            val reset = response.resources.core.reset
            Result.success(Pair(remaining, reset))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mapDtoToDomain(
        dto: GitHubRepoDto,
        isStarred: Boolean,
        isBookmarked: Boolean
    ): RepositoryItem {
        return RepositoryItem(
            id = dto.id,
            name = dto.name,
            fullName = dto.fullName,
            ownerLogin = dto.owner.login,
            ownerAvatarUrl = dto.owner.avatarUrl,
            description = dto.description,
            htmlUrl = dto.htmlUrl,
            starsCount = dto.stargazersCount,
            forksCount = dto.forksCount,
            language = dto.language,
            languageColor = RepositoryItem.getLanguageColor(dto.language),
            topics = dto.topics,
            licenseName = dto.license?.spdxId ?: dto.license?.name,
            createdAt = dto.createdAt,
            pushedAt = dto.pushedAt,
            isStarred = isStarred,
            isBookmarked = isBookmarked
        )
    }
}
