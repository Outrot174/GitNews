package ru.dev.gitnews.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.DynamicFeed
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.dev.gitnews.ui.theme.GitNewsTheme
import ru.dev.gitnews.ui.theme.hairlineBorder
import ru.dev.gitnews.ui.theme.tactileClick

enum class NavigationTab(val label: String, val icon: ImageVector) {
    FEED("Лента", Icons.Outlined.DynamicFeed),
    SEARCH("Поиск", Icons.Outlined.Search),
    BOOKMARKS("Избранное", Icons.Outlined.BookmarkBorder),
    PROFILE("Профиль", Icons.Outlined.Person)
}

@Composable
fun BottomNavBar(
    currentTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    onReselectTab: (NavigationTab) -> Unit = {}
) {
    val colors = GitNewsTheme.colors
    val typography = GitNewsTheme.typography

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceGlass)
            .hairlineBorder(colors.borderGhost, RoundedCornerShape(0.dp))
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavigationTab.entries.forEach { tab ->
                val isSelected = tab == currentTab
                val contentColor = if (isSelected) colors.textPrimary else colors.textTertiary

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .tactileClick {
                            if (isSelected) {
                                onReselectTab(tab)
                            } else {
                                onTabSelected(tab)
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = contentColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = tab.label,
                        style = typography.caption.copy(
                            fontSize = 11.sp,
                            color = contentColor
                        )
                    )
                }
            }
        }
    }
}
