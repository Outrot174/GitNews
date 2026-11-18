package ru.dev.gitnews.ui.screens.bookmarks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.dev.gitnews.domain.model.Bookmark
import ru.dev.gitnews.domain.model.RepositoryItem
import ru.dev.gitnews.domain.repository.BookmarkRepository
import ru.dev.gitnews.domain.repository.GitHubRepository

class BookmarksViewModel(
    private val bookmarkRepository: BookmarkRepository,
    private val gitHubRepository: GitHubRepository
) : ViewModel() {

    val bookmarks: StateFlow<List<Bookmark>> = bookmarkRepository.getBookmarksFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun removeBookmark(repoId: Long) {
        viewModelScope.launch {
            bookmarkRepository.removeBookmark(repoId)
        }
    }

    fun toggleStar(item: RepositoryItem) {
        viewModelScope.launch {
            if (item.isStarred) {
                gitHubRepository.unstarRepository(item.ownerLogin, item.name)
            } else {
                gitHubRepository.starRepository(item.ownerLogin, item.name)
            }
        }
    }
}
