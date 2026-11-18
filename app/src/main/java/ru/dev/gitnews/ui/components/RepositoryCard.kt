package ru.dev.gitnews.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CallSplit
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.dev.gitnews.domain.model.RepositoryItem
import ru.dev.gitnews.ui.theme.GitNewsTheme
import ru.dev.gitnews.ui.theme.hairlineBorder
import ru.dev.gitnews.ui.theme.tactileClick

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RepositoryCard(
    item: RepositoryItem,
    translatedDescription: String? = null,
    onCardClick: () -> Unit,
    onToggleStar: () -> Unit,
    onToggleBookmark: () -> Unit,
    onRequestTranslation: (String) -> Unit = {}
) {
    val colors = GitNewsTheme.colors
    val typography = GitNewsTheme.typography
    var showOriginalDesc by remember { mutableStateOf(false) }

    val starColor by animateColorAsState(
        targetValue = if (item.isStarred) colors.signalGold else colors.textTertiary,
        animationSpec = tween(durationMillis = 150),
        label = "starColorAnim"
    )

    val bookmarkColor by animateColorAsState(
        targetValue = if (item.isBookmarked) colors.signalEmerald else colors.textTertiary,
        animationSpec = tween(durationMillis = 150),
        label = "bookmarkColorAnim"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfacePanel)
            .hairlineBorder(colors.borderGhost, RoundedCornerShape(12.dp))
            .tactileClick(onClick = onCardClick)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

            // Header: Owner & Name (Avatar-free pure typography)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.fullName,
                    style = typography.title.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    ),
                    modifier = Modifier.weight(1f)
                )

                // Language Indicator
                if (!item.language.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        val langColor = remember(item.languageColor) {
                            try {
                                Color(android.graphics.Color.parseColor(item.languageColor ?: "#8A8F98"))
                            } catch (_: Exception) {
                                Color(0xFF8A8F98)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(langColor)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = item.language,
                            style = typography.caption.copy(
                                fontSize = 11.sp,
                                color = colors.textSecondary
                            )
                        )
                    }
                }
            }

            // Description Body
            val displayDescription = if (!showOriginalDesc && !translatedDescription.isNullOrBlank()) {
                translatedDescription
            } else {
                item.description
            }

            if (!displayDescription.isNullOrBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .tactileClick(scaleDown = 0.99f) {
                            if (!translatedDescription.isNullOrBlank()) {
                                showOriginalDesc = !showOriginalDesc
                            } else {
                                item.description?.let { onRequestTranslation(it) }
                            }
                        },
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = displayDescription,
                        style = typography.bodyMedium.copy(
                            color = colors.textSecondary,
                            lineHeight = 18.sp
                        ),
                        maxLines = 3,
                        modifier = Modifier.weight(1f)
                    )

                    if (!translatedDescription.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(colors.surfaceElevated)
                                .hairlineBorder(colors.borderGhost, RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (showOriginalDesc) "EN" else "RU",
                                style = typography.caption.copy(
                                    fontSize = 9.sp,
                                    color = if (showOriginalDesc) colors.textTertiary else colors.signalEmerald
                                )
                            )
                        }
                    }
                }
            }

            // Topics / Tags
            if (item.topics.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item.topics.take(4).forEach { topic ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(colors.surfaceSubtle)
                                .hairlineBorder(colors.borderGhost, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = topic,
                                style = typography.caption.copy(
                                    fontSize = 10.sp,
                                    color = colors.textSecondary
                                )
                            )
                        }
                    }
                }
            }

            // Telemetry Footer & Dual Actions (Star & Bookmark)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Telemetry (Stars, Forks, Time)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Star count
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.StarBorder,
                            contentDescription = null,
                            tint = colors.textTertiary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = formatCount(item.starsCount),
                            style = typography.monoData.copy(
                                fontSize = 12.sp,
                                color = colors.textSecondary
                            )
                        )
                    }

                    // Fork count
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.CallSplit,
                            contentDescription = null,
                            tint = colors.textTertiary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = formatCount(item.forksCount),
                            style = typography.monoData.copy(
                                fontSize = 12.sp,
                                color = colors.textSecondary
                            )
                        )
                    }

                    // Relative push time
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = colors.textTertiary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = item.relativeTime,
                            style = typography.monoData.copy(
                                fontSize = 11.sp,
                                color = colors.textTertiary
                            )
                        )
                    }
                }

                // Dual Action Buttons (GitHub Star & Local Favorite)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // 1. GitHub Star Button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .tactileClick(onClick = onToggleStar),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (item.isStarred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = "GitHub Star",
                            tint = starColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // 2. Local Favorite Button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .tactileClick(onClick = onToggleBookmark),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (item.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "В Избранное",
                            tint = bookmarkColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatCount(count: Int): String {
    return when {
        count >= 1000 -> "%.1fk".format(count / 1000.0)
        else -> count.toString()
    }
}
