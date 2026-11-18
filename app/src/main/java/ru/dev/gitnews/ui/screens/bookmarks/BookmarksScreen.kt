package ru.dev.gitnews.ui.screens.bookmarks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.dev.gitnews.domain.model.RepositoryItem
import ru.dev.gitnews.ui.components.RepositoryCard
import ru.dev.gitnews.ui.theme.GitNewsTheme

@Composable
fun BookmarksScreen(
    viewModel: BookmarksViewModel,
    onNavigateToDetail: (RepositoryItem) -> Unit
) {
    val colors = GitNewsTheme.colors
    val typography = GitNewsTheme.typography
    val bookmarks by viewModel.bookmarks.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surfaceCanvas)
    ) {
        if (bookmarks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "В Избранном пока пусто.\nНажмите на закладку в карточке репозитория.",
                    style = typography.bodyMedium.copy(
                        color = colors.textTertiary,
                        lineHeight = typography.bodyMedium.lineHeight
                    )
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(bookmarks, key = { it.repoId }) { bookmark ->
                    val repoItem = bookmark.toRepositoryItem()
                    RepositoryCard(
                        item = repoItem,
                        translatedDescription = bookmark.translatedDescription,
                        onCardClick = { onNavigateToDetail(repoItem) },
                        onToggleStar = { viewModel.toggleStar(repoItem) },
                        onToggleBookmark = { viewModel.removeBookmark(bookmark.repoId) }
                    )
                }
            }
        }
    }
}
