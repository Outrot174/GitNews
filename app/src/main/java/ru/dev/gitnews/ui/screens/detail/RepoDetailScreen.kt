package ru.dev.gitnews.ui.screens.detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CallSplit
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.dev.gitnews.ui.components.MarkdownView
import ru.dev.gitnews.ui.theme.GitNewsTheme
import ru.dev.gitnews.ui.theme.hairlineBorder
import ru.dev.gitnews.ui.theme.tactileClick

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RepoDetailScreen(
    viewModel: RepoDetailViewModel,
    onBackClick: () -> Unit,
    onTopicClick: (String) -> Unit
) {
    val colors = GitNewsTheme.colors
    val typography = GitNewsTheme.typography
    val context = LocalContext.current

    val detail by viewModel.detail.collectAsState()
    val releases by viewModel.releases.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isReadmeLoading by viewModel.isReadmeLoading.collectAsState()
    val readmeContent by viewModel.readmeContent.collectAsState()
    val readmeError by viewModel.readmeError.collectAsState()
    val isReadmeTranslated by viewModel.isReadmeTranslated.collectAsState()
    val isTranslating by viewModel.isTranslating.collectAsState()
    val translatedReadme by viewModel.translatedReadme.collectAsState()

    var showReleasesSheet by remember { mutableStateOf(false) }

    BackHandler(onBack = onBackClick)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surfaceCanvas)
            .statusBarsPadding()
    ) {
        if (isLoading) {
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
        } else {
            val item = detail?.item
            if (item == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Репозиторий не найден", style = typography.bodyMedium.copy(color = colors.textTertiary))
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    // Top Bar (Back button, Title, Open in Browser)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .tactileClick(onClick = onBackClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Назад",
                                tint = colors.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Text(
                            text = item.fullName,
                            style = typography.title.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            ),
                            maxLines = 1,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        )

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .tactileClick {
                                    val customTabs = CustomTabsIntent.Builder().build()
                                    customTabs.launchUrl(context, Uri.parse(item.htmlUrl))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.OpenInBrowser,
                                contentDescription = "Открыть в GitHub",
                                tint = colors.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Description Card
                    if (!item.description.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.surfacePanel)
                                .hairlineBorder(colors.borderGhost, RoundedCornerShape(8.dp))
                                .padding(14.dp)
                        ) {
                            Text(
                                text = item.description,
                                style = typography.bodyLarge.copy(
                                    color = colors.textPrimary,
                                    fontSize = 14.sp
                                )
                            )
                        }
                    }

                    // Clickable Topics
                    if (item.topics.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            item.topics.forEach { topic ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(colors.surfaceElevated)
                                        .hairlineBorder(colors.borderGhost, RoundedCornerShape(4.dp))
                                        .tactileClick { onTopicClick(topic) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "#$topic",
                                        style = typography.caption.copy(
                                            fontSize = 11.sp,
                                            color = colors.textSecondary
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Quick Actions Row: Star, Favorite & Releases Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Star Button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.surfacePanel)
                                .hairlineBorder(
                                    if (item.isStarred) colors.signalGold else colors.borderSubtle,
                                    RoundedCornerShape(8.dp)
                                )
                                .tactileClick { viewModel.toggleStar() },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (item.isStarred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                    contentDescription = null,
                                    tint = if (item.isStarred) colors.signalGold else colors.textPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (item.isStarred) "В звездах" else "Star",
                                    style = typography.caption.copy(
                                        color = colors.textPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }

                        // 2. Favorite Button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.surfacePanel)
                                .hairlineBorder(
                                    if (item.isBookmarked) colors.signalEmerald else colors.borderSubtle,
                                    RoundedCornerShape(8.dp)
                                )
                                .tactileClick { viewModel.toggleBookmark() },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (item.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                    contentDescription = null,
                                    tint = if (item.isBookmarked) colors.signalEmerald else colors.textPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (item.isBookmarked) "Сохранено" else "В Избранное",
                                    style = typography.caption.copy(
                                        color = colors.textPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }

                        // 3. Prominent "Релизы" (Releases) Button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.surfaceElevated)
                                .hairlineBorder(colors.borderAccent, RoundedCornerShape(8.dp))
                                .tactileClick { showReleasesSheet = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.NewReleases,
                                    contentDescription = null,
                                    tint = colors.signalCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Релизы (${releases.size})",
                                    style = typography.caption.copy(
                                        color = colors.textPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }

                        // 4. Clone Button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.surfacePanel)
                                .hairlineBorder(colors.borderSubtle, RoundedCornerShape(8.dp))
                                .tactileClick {
                                    val cloneCmd = "git clone https://github.com/${item.fullName}.git"
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    clipboard?.setPrimaryClip(ClipData.newPlainText("git clone command", cloneCmd))
                                    Toast.makeText(context, "Команда git clone скопирована", Toast.LENGTH_SHORT).show()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = "Клонировать репозиторий",
                                    tint = colors.textPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Clone",
                                    style = typography.caption.copy(
                                        color = colors.textPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }
                    }

                    // Telemetry Stats Row
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.surfaceSubtle)
                            .hairlineBorder(colors.borderGhost, RoundedCornerShape(8.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Звезды", style = typography.caption.copy(color = colors.textTertiary))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(item.starsCount.toString(), style = typography.monoData.copy(color = colors.textPrimary))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Форки", style = typography.caption.copy(color = colors.textTertiary))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(item.forksCount.toString(), style = typography.monoData.copy(color = colors.textPrimary))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Issues", style = typography.caption.copy(color = colors.textTertiary))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(detail?.openIssuesCount?.toString() ?: "0", style = typography.monoData.copy(color = colors.textPrimary))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Ветка", style = typography.caption.copy(color = colors.textTertiary))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(detail?.defaultBranch ?: "main", style = typography.monoData.copy(color = colors.textPrimary))
                            }
                        }
                    }

                    // README Section Header with Translation Switcher
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "README.md",
                            style = typography.title.copy(
                                fontSize = 16.sp,
                                color = colors.textPrimary
                            )
                        )

                        // Translate Toggle Pill
                        Box(
                            modifier = Modifier
                                .height(32.dp)
                                .clip(RoundedCornerShape(9999.dp))
                                .background(if (isReadmeTranslated) colors.surfaceElevated else colors.surfaceSubtle)
                                .hairlineBorder(
                                    if (isReadmeTranslated) colors.borderAccent else colors.borderGhost,
                                    RoundedCornerShape(9999.dp)
                                )
                                .tactileClick { viewModel.toggleTranslateReadme() }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isTranslating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        strokeWidth = 2.dp,
                                        color = colors.signalEmerald
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.Translate,
                                        contentDescription = null,
                                        tint = if (isReadmeTranslated) colors.signalEmerald else colors.textTertiary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = if (isReadmeTranslated) "Переведено (RU)" else "Перевести (RU)",
                                    style = typography.caption.copy(
                                        fontSize = 11.sp,
                                        color = if (isReadmeTranslated) colors.signalEmerald else colors.textSecondary
                                    )
                                )
                            }
                        }
                    }

                    // Rendered README
                    val rawContent = readmeContent ?: detail?.rawReadmeContent
                    val readmeToDisplay = if (isReadmeTranslated && !translatedReadme.isNullOrBlank()) {
                        translatedReadme
                    } else {
                        rawContent
                    }

                    when {
                        isReadmeLoading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.surfaceSubtle)
                                    .hairlineBorder(colors.borderGhost, RoundedCornerShape(8.dp))
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = colors.signalEmerald,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = "Загрузка README.md...",
                                        style = typography.caption.copy(color = colors.textSecondary)
                                    )
                                }
                            }
                        }

                        !readmeError.isNullOrBlank() && readmeToDisplay.isNullOrBlank() -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.surfaceSubtle)
                                    .hairlineBorder(colors.borderGhost, RoundedCornerShape(8.dp))
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = readmeError ?: "Не удалось загрузить README.md",
                                        style = typography.bodyMedium.copy(color = colors.textSecondary),
                                        textAlign = TextAlign.Center
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(colors.surfaceElevated)
                                            .hairlineBorder(colors.borderSubtle, RoundedCornerShape(6.dp))
                                            .tactileClick { viewModel.retryReadme() }
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Outlined.Refresh,
                                                contentDescription = null,
                                                tint = colors.textPrimary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Повторить",
                                                style = typography.caption.copy(
                                                    color = colors.textPrimary,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        readmeToDisplay.isNullOrBlank() -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.surfaceSubtle)
                                    .hairlineBorder(colors.borderGhost, RoundedCornerShape(8.dp))
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "README отсутствует",
                                    style = typography.bodyMedium.copy(color = colors.textTertiary)
                                )
                            }
                        }

                        else -> {
                            MarkdownView(
                                markdown = readmeToDisplay,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }

        // Releases Bottom Sheet
        if (showReleasesSheet) {
            ReleasesBottomSheet(
                releases = releases,
                onDismiss = { showReleasesSheet = false }
            )
        }
    }
}
