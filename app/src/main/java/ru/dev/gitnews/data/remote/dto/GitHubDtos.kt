package ru.dev.gitnews.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubSearchResponseDto(
    @SerialName("total_count") val totalCount: Int = 0,
    @SerialName("incomplete_results") val incompleteResults: Boolean = false,
    @SerialName("items") val items: List<GitHubRepoDto> = emptyList()
)

@Serializable
data class GitHubRepoDto(
    @SerialName("id") val id: Long,
    @SerialName("name") val name: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("owner") val owner: GitHubOwnerDto,
    @SerialName("description") val description: String? = null,
    @SerialName("html_url") val htmlUrl: String,
    @SerialName("stargazers_count") val stargazersCount: Int = 0,
    @SerialName("forks_count") val forksCount: Int = 0,
    @SerialName("open_issues_count") val openIssuesCount: Int = 0,
    @SerialName("subscribers_count") val subscribersCount: Int = 0,
    @SerialName("language") val language: String? = null,
    @SerialName("topics") val topics: List<String> = emptyList(),
    @SerialName("license") val license: GitHubLicenseDto? = null,
    @SerialName("default_branch") val defaultBranch: String = "main",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("pushed_at") val pushedAt: String? = null
)

@Serializable
data class GitHubOwnerDto(
    @SerialName("id") val id: Long = 0,
    @SerialName("login") val login: String = "",
    @SerialName("avatar_url") val avatarUrl: String = ""
)

@Serializable
data class GitHubLicenseDto(
    @SerialName("key") val key: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("spdx_id") val spdxId: String? = null
)

@Serializable
data class GitHubUserDto(
    @SerialName("id") val id: Long = 0,
    @SerialName("login") val login: String = "",
    @SerialName("name") val name: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("bio") val bio: String? = null,
    @SerialName("public_repos") val publicRepos: Int = 0,
    @SerialName("followers") val followers: Int = 0,
    @SerialName("following") val following: Int = 0
)

@Serializable
data class DeviceCodeResponseDto(
    @SerialName("device_code") val deviceCode: String = "",
    @SerialName("user_code") val userCode: String = "",
    @SerialName("verification_uri") val verificationUri: String = "https://github.com/login/device",
    @SerialName("expires_in") val expiresIn: Int = 900,
    @SerialName("interval") val interval: Int = 5
)

@Serializable
data class OAuthTokenResponseDto(
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("token_type") val tokenType: String? = null,
    @SerialName("scope") val scope: String? = null,
    @SerialName("error") val error: String? = null,
    @SerialName("error_description") val errorDescription: String? = null,
    @SerialName("interval") val interval: Int? = null
)

@Serializable
data class ReleaseDto(
    @SerialName("id") val id: Long,
    @SerialName("tag_name") val tagName: String,
    @SerialName("name") val name: String? = null,
    @SerialName("body") val body: String? = null,
    @SerialName("published_at") val publishedAt: String? = null,
    @SerialName("html_url") val htmlUrl: String,
    @SerialName("prerelease") val prerelease: Boolean = false,
    @SerialName("assets") val assets: List<ReleaseAssetDto> = emptyList()
)

@Serializable
data class ReleaseAssetDto(
    @SerialName("id") val id: Long,
    @SerialName("name") val name: String,
    @SerialName("size") val size: Long = 0,
    @SerialName("download_count") val downloadCount: Int = 0,
    @SerialName("browser_download_url") val browserDownloadUrl: String
)

@Serializable
data class RateLimitResponseDto(
    @SerialName("resources") val resources: RateLimitResourcesDto
)

@Serializable
data class RateLimitResourcesDto(
    @SerialName("core") val core: RateLimitCategoryDto,
    @SerialName("search") val search: RateLimitCategoryDto
)

@Serializable
data class RateLimitCategoryDto(
    @SerialName("limit") val limit: Int = 0,
    @SerialName("remaining") val remaining: Int = 0,
    @SerialName("reset") val reset: Long = 0
)
