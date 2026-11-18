package ru.dev.gitnews.domain.model

enum class BuiltInFeedType(val title: String) {
    TRENDING("Тренды"),
    RECENT("Недавние")
}

enum class FeedSortOption(
    val label: String,
    val serverSort: String? = null,
    val serverOrder: String? = null
) {
    STARS_DESC("По звёздам (убывание)", "stars", "desc"),
    STARS_ASC("По звёздам (возрастание)", "stars", "asc"),
    CREATED_DESC("От новых к старым", null, null),
    CREATED_ASC("От старых к новым", null, null),
    NAME_ASC("По алфавиту (A–Z)", null, null),
    NAME_DESC("По алфавиту (Z–A)", null, null),
    UPDATED_DESC("Недавно обновлённые", "updated", "desc"),
    FORKS_DESC("По форкам (убывание)", "forks", "desc")
}
