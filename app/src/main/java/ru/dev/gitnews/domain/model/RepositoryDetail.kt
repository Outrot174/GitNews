package ru.dev.gitnews.domain.model

data class RepositoryDetail(
    val item: RepositoryItem,
    val defaultBranch: String = "main",
    val openIssuesCount: Int = 0,
    val subscribersCount: Int = 0,
    val rawReadmeContent: String? = null,
    val translatedReadmeContent: String? = null,
    val latestRelease: ReleaseItem? = null,
    val totalReleasesCount: Int = 0
)
