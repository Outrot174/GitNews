package ru.dev.gitnews.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.dev.gitnews.domain.model.RepositoryItem
import ru.dev.gitnews.ui.components.RepositoryCard
import ru.dev.gitnews.ui.theme.GitNewsTheme
import ru.dev.gitnews.ui.theme.hairlineBorder
import ru.dev.gitnews.ui.theme.tactileClick

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onNavigateToDetail: (RepositoryItem) -> Unit
) {
    val colors = GitNewsTheme.colors
    val typography = GitNewsTheme.typography

    val query by viewModel.query.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val recentQueries by viewModel.recentQueries.collectAsState()
    val results by viewModel.results.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()

    val listState = rememberLazyListState()

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

    var showSavePresetDialog by remember { mutableStateOf(false) }
    var presetTitle by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surfaceCanvas)
    ) {
        // Search Input Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = viewModel::onQueryChanged,
                    placeholder = { Text("Поиск (например: compose agent)", color = colors.textTertiary, fontSize = 13.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { viewModel.performSearch() }),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = colors.textTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedBorderColor = colors.borderAccent,
                        unfocusedBorderColor = colors.borderSubtle,
                        focusedContainerColor = colors.surfaceSubtle,
                        unfocusedContainerColor = colors.surfaceSubtle
                    )
                )

                if (query.isNotBlank() || selectedLanguage != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.surfaceElevated)
                            .hairlineBorder(colors.borderSubtle, RoundedCornerShape(8.dp))
                            .tactileClick {
                                presetTitle = query.ifBlank { selectedLanguage ?: "Пресет" }
                                showSavePresetDialog = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.BookmarkAdd,
                            contentDescription = "Сохранить как пресет",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Quick Language Chips
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                viewModel.quickLanguages.forEach { lang ->
                    val isSelected = selectedLanguage == lang
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9999.dp))
                            .background(if (isSelected) colors.surfaceElevated else colors.surfaceSubtle)
                            .hairlineBorder(
                                color = if (isSelected) colors.borderAccent else colors.borderGhost,
                                shape = RoundedCornerShape(9999.dp)
                            )
                            .tactileClick { viewModel.selectLanguage(lang) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = lang,
                            style = typography.caption.copy(
                                fontSize = 11.sp,
                                color = if (isSelected) colors.textPrimary else colors.textSecondary
                            )
                        )
                    }
                }
            }

            // Recent Search Chips (when history exists and query is empty or being composed)
            if (recentQueries.isNotEmpty() && query.isBlank()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Недавние поиски",
                            style = typography.caption.copy(fontSize = 11.sp, color = colors.textTertiary)
                        )
                        Text(
                            text = "Очистить",
                            style = typography.caption.copy(fontSize = 11.sp, color = colors.textSecondary),
                            modifier = Modifier.tactileClick { viewModel.clearRecentQueries() }
                        )
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        recentQueries.forEach { histQuery ->
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(9999.dp))
                                    .background(colors.surfaceSubtle)
                                    .hairlineBorder(colors.borderGhost, RoundedCornerShape(9999.dp))
                                    .tactileClick {
                                        viewModel.onQueryChanged(histQuery)
                                        viewModel.performSearch()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = histQuery,
                                    style = typography.caption.copy(fontSize = 11.sp, color = colors.textPrimary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Удалить",
                                    tint = colors.textTertiary,
                                    modifier = Modifier
                                        .size(12.dp)
                                        .tactileClick { viewModel.deleteRecentQuery(histQuery) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Search Results Area with PullToRefreshBox
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = { viewModel.performSearch() },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when {
                results.isEmpty() && !isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (query.isBlank() && selectedLanguage == null) {
                                "Введите запрос или выберите язык для поиска"
                            } else {
                                "Ничего не найдено"
                            },
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
                        items(results, key = { it.id }) { repo ->
                            RepositoryCard(
                                item = repo,
                                onCardClick = { onNavigateToDetail(repo) },
                                onToggleStar = { viewModel.toggleStar(repo) },
                                onToggleBookmark = { viewModel.toggleBookmark(repo) }
                            )
                        }

                        if (isLoadingMore) {
                            item(key = "search_pagination_loader") {
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

    // Save Preset Dialog
    if (showSavePresetDialog) {
        AlertDialog(
            onDismissRequest = { showSavePresetDialog = false },
            title = { Text("Сохранить поиск как пресет", color = colors.textPrimary) },
            text = {
                OutlinedTextField(
                    value = presetTitle,
                    onValueChange = { presetTitle = it },
                    label = { Text("Название пресета") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveCurrentSearchAsPreset(presetTitle)
                        showSavePresetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.textPrimary)
                ) {
                    Text("Сохранить", color = colors.textInverse)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showSavePresetDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceSubtle)
                ) {
                    Text("Отмена", color = colors.textSecondary)
                }
            },
            containerColor = colors.surfacePanel
        )
    }
}
