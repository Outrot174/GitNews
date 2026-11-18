package ru.dev.gitnews.ui.components

import android.widget.ImageView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.dev.gitnews.ui.screens.feed.TranslationMode
import ru.dev.gitnews.ui.theme.GitNewsTheme
import ru.dev.gitnews.ui.theme.hairlineBorder
import ru.dev.gitnews.ui.theme.tactileClick

@Composable
fun TopBar(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    translationMode: TranslationMode = TranslationMode.ALWAYS,
    onSelectTranslationMode: (TranslationMode) -> Unit = {},
    onAppIconClick: () -> Unit = {}
) {
    val colors = GitNewsTheme.colors
    val typography = GitNewsTheme.typography
    var showTranslationMenu by remember { mutableStateOf(false) }

    val rotation by animateFloatAsState(
        targetValue = if (isDarkTheme) 0f else 180f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
        label = "themeIconRotation"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceGlass)
            .statusBarsPadding()
            .hairlineBorder(colors.borderGhost, RoundedCornerShape(0.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Application Launcher Icon (rendered via AndroidView to support AdaptiveIconDrawable without Compose painterResource crash)
            AndroidView(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .tactileClick { onAppIconClick() },
                factory = { ctx ->
                    ImageView(ctx).apply {
                        setImageResource(ru.dev.gitnews.R.mipmap.ic_launcher)
                        scaleType = ImageView.ScaleType.FIT_CENTER
                    }
                }
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Translation Mode Capsule
                Box {
                    Row(
                        modifier = Modifier
                            .height(30.dp)
                            .clip(RoundedCornerShape(9999.dp))
                            .background(colors.surfaceElevated)
                            .hairlineBorder(colors.borderSubtle, RoundedCornerShape(9999.dp))
                            .tactileClick { showTranslationMenu = true }
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (translationMode != TranslationMode.OFF) {
                                        colors.signalEmerald
                                    } else {
                                        colors.textTertiary
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when (translationMode) {
                                TranslationMode.ALWAYS -> "RU Авто"
                                TranslationMode.ON_REQUEST -> "RU Запрос"
                                TranslationMode.OFF -> "Оригинал"
                            },
                            style = typography.caption.copy(
                                color = colors.textPrimary,
                                fontSize = 11.sp
                            )
                        )
                    }

                    DropdownMenu(
                        expanded = showTranslationMenu,
                        onDismissRequest = { showTranslationMenu = false },
                        modifier = Modifier
                            .background(colors.surfacePanel)
                            .hairlineBorder(colors.borderStrong, RoundedCornerShape(8.dp))
                    ) {
                        DropdownMenuItem(
                            text = { Text("Всегда переводить (RU Авто)", color = colors.textPrimary) },
                            onClick = {
                                onSelectTranslationMode(TranslationMode.ALWAYS)
                                showTranslationMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("По нажатию (RU Запрос)", color = colors.textPrimary) },
                            onClick = {
                                onSelectTranslationMode(TranslationMode.ON_REQUEST)
                                showTranslationMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Только оригинал (EN/Source)", color = colors.textPrimary) },
                            onClick = {
                                onSelectTranslationMode(TranslationMode.OFF)
                                showTranslationMenu = false
                            }
                        )
                    }
                }

                // Theme Toggle Icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .tactileClick(onClick = onToggleTheme),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Outlined.DarkMode else Icons.Outlined.LightMode,
                        contentDescription = "Сменить тему",
                        tint = colors.textPrimary,
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(rotation)
                    )
                }
            }
        }
    }
}
