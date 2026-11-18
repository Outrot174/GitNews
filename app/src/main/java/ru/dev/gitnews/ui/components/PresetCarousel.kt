package ru.dev.gitnews.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.dev.gitnews.domain.model.BuiltInFeedType
import ru.dev.gitnews.domain.model.FeedSortOption
import ru.dev.gitnews.domain.model.Preset
import ru.dev.gitnews.ui.theme.GitNewsTheme
import ru.dev.gitnews.ui.theme.hairlineBorder
import ru.dev.gitnews.ui.theme.tactileClick

@Composable
fun PresetCarousel(
    presets: List<Preset>,
    selectedPreset: Preset?,
    selectedBuiltIn: BuiltInFeedType?,
    onSelectBuiltIn: (BuiltInFeedType) -> Unit,
    onSelectPreset: (Preset?) -> Unit,
    onCreatePreset: (Preset) -> Unit,
    onDeletePreset: (String) -> Unit,
    currentSort: FeedSortOption,
    onSelectSort: (FeedSortOption) -> Unit
) {
    val colors = GitNewsTheme.colors
    val typography = GitNewsTheme.typography
    var showCreateDialog by remember { mutableStateOf(false) }
    var presetToDelete by remember { mutableStateOf<Preset?>(null) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LazyRow(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // "Тренды" built-in pill
            item {
                val isSelected = selectedBuiltIn == BuiltInFeedType.TRENDING && selectedPreset == null
                Box(
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(9999.dp))
                        .background(if (isSelected) colors.surfaceElevated else colors.surfaceSubtle)
                        .hairlineBorder(
                            color = if (isSelected) colors.borderAccent else colors.borderGhost,
                            shape = RoundedCornerShape(9999.dp)
                        )
                        .tactileClick { onSelectBuiltIn(BuiltInFeedType.TRENDING) }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Тренды",
                        style = typography.caption.copy(
                            fontSize = 12.sp,
                            color = if (isSelected) colors.textPrimary else colors.textSecondary
                        )
                    )
                }
            }

            // "Недавние" built-in pill
            item {
                val isSelected = selectedBuiltIn == BuiltInFeedType.RECENT && selectedPreset == null
                Box(
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(9999.dp))
                        .background(if (isSelected) colors.surfaceElevated else colors.surfaceSubtle)
                        .hairlineBorder(
                            color = if (isSelected) colors.borderAccent else colors.borderGhost,
                            shape = RoundedCornerShape(9999.dp)
                        )
                        .tactileClick { onSelectBuiltIn(BuiltInFeedType.RECENT) }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Недавние",
                        style = typography.caption.copy(
                            fontSize = 12.sp,
                            color = if (isSelected) colors.textPrimary else colors.textSecondary
                        )
                    )
                }
            }

            // Custom Presets List
            items(presets, key = { it.id }) { preset ->
                val isSelected = selectedPreset?.id == preset.id
                Row(
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(9999.dp))
                        .background(if (isSelected) colors.surfaceElevated else colors.surfaceSubtle)
                        .hairlineBorder(
                            color = if (isSelected) colors.borderAccent else colors.borderGhost,
                            shape = RoundedCornerShape(9999.dp)
                        )
                        .tactileClick { onSelectPreset(preset) }
                        .padding(start = 14.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = preset.title,
                        style = typography.caption.copy(
                            fontSize = 12.sp,
                            color = if (isSelected) colors.textPrimary else colors.textSecondary
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(9999.dp))
                            .tactileClick { presetToDelete = preset },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Удалить пресет",
                            tint = colors.textTertiary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // "+ Пресет" Button
            item {
                Row(
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(9999.dp))
                        .background(colors.surfaceCanvas)
                        .hairlineBorder(colors.borderSubtle, RoundedCornerShape(9999.dp))
                        .tactileClick { showCreateDialog = true }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = "Создать пресет",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Пресет",
                        style = typography.caption.copy(
                            fontSize = 12.sp,
                            color = colors.textPrimary
                        )
                    )
                }
            }
        }

        // Pinned "Сортировать" Button & Dropdown
        Box(
            modifier = Modifier.padding(end = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .height(34.dp)
                    .clip(RoundedCornerShape(9999.dp))
                    .background(colors.surfaceCanvas)
                    .hairlineBorder(colors.borderSubtle, RoundedCornerShape(9999.dp))
                    .tactileClick { sortMenuExpanded = true }
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Sort,
                    contentDescription = "Сортировать",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Сортировать",
                    style = typography.caption.copy(
                        fontSize = 12.sp,
                        color = colors.textPrimary
                    )
                )
            }

            DropdownMenu(
                expanded = sortMenuExpanded,
                onDismissRequest = { sortMenuExpanded = false },
                modifier = Modifier
                    .background(colors.surfacePanel)
                    .hairlineBorder(colors.borderSubtle, RoundedCornerShape(12.dp))
            ) {
                FeedSortOption.entries.forEach { option ->
                    val isCurrent = option == currentSort
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option.label,
                                style = typography.caption.copy(
                                    fontSize = 13.sp,
                                    color = if (isCurrent) colors.signalEmerald else colors.textPrimary
                                )
                            )
                        },
                        onClick = {
                            onSelectSort(option)
                            sortMenuExpanded = false
                        },
                        modifier = Modifier.background(
                            if (isCurrent) colors.surfaceSubtle else colors.surfacePanel
                        )
                    )
                }
            }
        }
    }


    // Create Preset Dialog
    if (showCreateDialog) {
        CreatePresetDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { newPreset ->
                onCreatePreset(newPreset)
                showCreateDialog = false
            }
        )
    }

    // Delete Confirmation Dialog
    if (presetToDelete != null) {
        AlertDialog(
            onDismissRequest = { presetToDelete = null },
            title = { Text("Удалить пресет?", color = colors.textPrimary) },
            text = { Text("Пресет «${presetToDelete?.title}» будет удален.", color = colors.textSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        presetToDelete?.let { onDeletePreset(it.id) }
                        presetToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.signalCrimson)
                ) {
                    Text("Удалить", color = colors.textPrimary)
                }
            },
            dismissButton = {
                Button(
                    onClick = { presetToDelete = null },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceSubtle)
                ) {
                    Text("Отмена", color = colors.textSecondary)
                }
            },
            containerColor = colors.surfacePanel
        )
    }
}

@Composable
fun CreatePresetDialog(
    onDismiss: () -> Unit,
    onConfirm: (Preset) -> Unit
) {
    val colors = GitNewsTheme.colors
    var title by remember { mutableStateOf("") }
    var keywords by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("") }
    var minStarsStr by remember { mutableStateOf("50") }
    var pushedDaysStr by remember { mutableStateOf("7") }
    var noForks by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый пресет", color = colors.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название (например: AI Kotlin)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedBorderColor = colors.borderAccent,
                        unfocusedBorderColor = colors.borderSubtle
                    )
                )
                OutlinedTextField(
                    value = keywords,
                    onValueChange = { keywords = it },
                    label = { Text("Ключевые слова (через запятую)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedBorderColor = colors.borderAccent,
                        unfocusedBorderColor = colors.borderSubtle
                    )
                )
                OutlinedTextField(
                    value = language,
                    onValueChange = { language = it },
                    label = { Text("Язык (например: Kotlin, Python)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedBorderColor = colors.borderAccent,
                        unfocusedBorderColor = colors.borderSubtle
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Мин. звезд:", color = colors.textSecondary)
                    OutlinedTextField(
                        value = minStarsStr,
                        onValueChange = { minStarsStr = it },
                        singleLine = true,
                        modifier = Modifier.width(100.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        )
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Обновлен за (дней):", color = colors.textSecondary)
                    OutlinedTextField(
                        value = pushedDaysStr,
                        onValueChange = { pushedDaysStr = it },
                        singleLine = true,
                        modifier = Modifier.width(100.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        )
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Без форков:", color = colors.textSecondary)
                    Switch(
                        checked = noForks,
                        onCheckedChange = { noForks = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = colors.signalEmerald)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val langs = language.split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                        val preset = Preset(
                            title = title.trim(),
                            queryKeywords = keywords.trim(),
                            languages = langs,
                            minStars = minStarsStr.toIntOrNull() ?: 0,
                            pushedWithinDays = pushedDaysStr.toIntOrNull() ?: 7,
                            excludeForks = noForks
                        )
                        onConfirm(preset)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.textPrimary)
            ) {
                Text("Сохранить", color = colors.textInverse)
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceSubtle)
            ) {
                Text("Отмена", color = colors.textSecondary)
            }
        },
        containerColor = colors.surfacePanel
    )
}
