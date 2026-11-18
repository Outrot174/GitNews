package ru.dev.gitnews.domain.model

data class ReleaseItem(
    val id: Long,
    val tagName: String,
    val name: String,
    val body: String?,
    val publishedAt: String?,
    val htmlUrl: String,
    val isPrerelease: Boolean = false,
    val assets: List<ReleaseAsset> = emptyList()
) {
    val relativeTime: String
        get() = RepositoryItem.calculateRelativeTime(publishedAt)
}

data class ReleaseAsset(
    val id: Long,
    val name: String,
    val sizeBytes: Long,
    val downloadCount: Int,
    val browserDownloadUrl: String
) {
    val formattedSize: String
        get() {
            if (sizeBytes < 1024) return "$sizeBytes B"
            val kb = sizeBytes / 1024.0
            if (kb < 1024) return "%.1f KB".format(kb)
            val mb = kb / 1024.0
            return "%.1f MB".format(mb)
        }
}
