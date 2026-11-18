package ru.dev.gitnews.ui.screens.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.dev.gitnews.domain.model.RepositoryItem
import ru.dev.gitnews.ui.components.PresetCarousel
import ru.dev.gitnews.ui.components.RepositoryCard
import ru.dev.gitnews.ui.theme.GitNewsTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    listState: LazyListState = rememberLazyListState(),
    onNavigateToDetail: (RepositoryItem) -> Unit
) {
    val colors = GitNewsTheme.colors
    val typography = GitNewsTheme.typography

    val presets by viewModel.presets.collectAsState()
    val selectedPreset by viewModel.selectedPreset.collectAsState()
    val selectedBuiltIn by viewModel.selectedBuiltIn.collectAsState()
    val currentSort by viewModel.currentSort.collectAsState()
    val items by viewModel.items.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val translatedDescriptions by viewModel.translatedDescriptions.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val total = layoutInfo.totalItemsCount
            val last = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            total to last
        }.collect { (total, last) ->
            if (total > 0 && last >= total - 5) {
                viewModel.loadNextPage()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surfaceCanvas)
    ) {
        // Presets Carousel
        PresetCarousel(
            presets = presets,
            selectedPreset = selectedPreset,
            selectedBuiltIn = selectedBuiltIn,
            onSelectBuiltIn = viewModel::selectBuiltIn,
            onSelectPreset = viewModel::selectPreset,
            onCreatePreset = viewModel::createPreset,
            onDeletePreset = viewModel::deletePreset,
            currentSort = currentSort,
            onSelectSort = viewModel::setSortOption
        )

        // Main Feed Content with PullToRefresh
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshFeed() },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when {
                isLoading && items.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = colors.textPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                !errorMessage.isNullOrBlank() && items.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            style = typography.bodyMedium.copy(color = colors.signalCrimson)
                        )
                    }
                }

                items.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Репозитории не найдены",
                            style = typography.bodyMedium.copy(color = colors.textTertiary)
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(items, key = { it.id }) { repo ->
                            RepositoryCard(
                                item = repo,
                                translatedDescription = translatedDescriptions[repo.id],
                                onCardClick = { onNavigateToDetail(repo) },
                                onToggleStar = { viewModel.toggleStar(repo) },
                                onToggleBookmark = { viewModel.toggleBookmark(repo) },
                                onRequestTranslation = { rawDesc ->
                                    viewModel.requestTranslation(repo.id, rawDesc)
                                }
                            )
                        }

                        if (isLoadingMore) {
                            item(key = "feed_pagination_loader") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = colors.textSecondary,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
