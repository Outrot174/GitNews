package ru.dev.gitnews.ui.screens.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import ru.dev.gitnews.domain.model.AuthProvider
import ru.dev.gitnews.ui.screens.auth.GoogleGlyph
import ru.dev.gitnews.ui.theme.GitNewsTheme
import ru.dev.gitnews.ui.theme.hairlineBorder
import ru.dev.gitnews.ui.theme.tactileClick

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel
) {
    val colors = GitNewsTheme.colors
    val typography = GitNewsTheme.typography
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    val authProvider by viewModel.authProvider.collectAsState()
    val userLogin by viewModel.userLogin.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState()
    val userAvatar by viewModel.userAvatar.collectAsState()
    val linkedGitHubToken by viewModel.linkedGitHubToken.collectAsState()
    val patInput by viewModel.patInput.collectAsState()
    val tokenMessage by viewModel.tokenMessage.collectAsState()
    val isLinking by viewModel.isLinking.collectAsState()
    val rateLimitRemaining by viewModel.rateLimitRemaining.collectAsState()
    val rateLimitResetTime by viewModel.rateLimitResetTime.collectAsState()
    val liveRateLimit by viewModel.liveRateLimit.collectAsState()
    val isNotificationsEnabled by viewModel.isNotificationsEnabled.collectAsState()

    var showLogoutDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surfaceCanvas)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // User Profile Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surfacePanel)
                .hairlineBorder(colors.borderGhost, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!userAvatar.isNullOrBlank()) {
                    AsyncImage(
                        model = userAvatar,
                        contentDescription = "Аватар",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .hairlineBorder(colors.borderSubtle, CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceElevated)
                            .hairlineBorder(colors.borderSubtle, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = userName ?: userLogin,
                            style = typography.title.copy(color = colors.textPrimary)
                        )

                        // Auth Provider Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(colors.surfaceCanvas)
                                .hairlineBorder(colors.borderGhost, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                when (authProvider) {
                                    AuthProvider.GOOGLE -> {
                                        GoogleGlyph(modifier = Modifier.size(10.dp), tint = colors.signalCyan)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Google", style = typography.caption.copy(fontSize = 10.sp, color = colors.signalCyan))
                                    }
                                    AuthProvider.EMAIL -> {
                                        Icon(Icons.Outlined.Email, null, tint = colors.signalAmber, modifier = Modifier.size(10.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Email", style = typography.caption.copy(fontSize = 10.sp, color = colors.signalAmber))
                                    }
                                    AuthProvider.GITHUB -> {
                                        Icon(Icons.Outlined.Code, null, tint = colors.signalEmerald, modifier = Modifier.size(10.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("GitHub", style = typography.caption.copy(fontSize = 10.sp, color = colors.signalEmerald))
                                    }
                                }
                            }
                        }
                    }

                    if (!userEmail.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = userEmail ?: "",
                            style = typography.caption.copy(color = colors.textTertiary, fontSize = 12.sp)
                        )
                    } else if (!userName.isNullOrBlank() && userName != userLogin) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "@$userLogin",
                            style = typography.caption.copy(color = colors.textTertiary, fontSize = 12.sp)
                        )
                    }
                }
            }
        }

        // GitHub Token Linking Card (For Google and Email users)
        if (authProvider != AuthProvider.GITHUB) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surfacePanel)
                    .hairlineBorder(colors.borderGhost, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Star,
                            contentDescription = null,
                            tint = colors.signalAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Привязка GitHub (Звезды и лимиты)",
                            style = typography.title.copy(fontSize = 14.sp, color = colors.textPrimary)
                        )
                    }

                    Text(
                        text = "Для возможности ставить звезды репозиториям и увеличения лимита API до 5000 запросов/час привяжите Personal Access Token (PAT).",
                        style = typography.caption.copy(color = colors.textSecondary, fontSize = 12.sp)
                    )

                    if (!tokenMessage.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(colors.surfaceCanvas)
                                .hairlineBorder(colors.borderSubtle, RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = tokenMessage ?: "",
                                style = typography.caption.copy(
                                    color = if (tokenMessage?.contains("успешно") == true) colors.signalEmerald else colors.signalCrimson,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    if (!linkedGitHubToken.isNullOrBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.surfaceCanvas)
                                .hairlineBorder(colors.signalEmerald, RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = colors.signalEmerald,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "GitHub токен привязан",
                                    style = typography.caption.copy(
                                        color = colors.textPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(colors.surfaceElevated)
                                    .hairlineBorder(colors.borderGhost, RoundedCornerShape(6.dp))
                                    .tactileClick { viewModel.unlinkGitHubToken() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.LinkOff,
                                        contentDescription = null,
                                        tint = colors.signalCrimson,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Отвязать",
                                        style = typography.caption.copy(
                                            color = colors.signalCrimson,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = patInput,
                            onValueChange = viewModel::onPatInputChanged,
                            placeholder = { Text("ghp_xxxxxxxxxxxxxxxxxxxx", color = colors.textTertiary, fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                focusManager.clearFocus()
                                viewModel.linkGitHubToken()
                            }),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Key,
                                    contentDescription = null,
                                    tint = colors.textTertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary,
                                focusedBorderColor = colors.borderAccent,
                                unfocusedBorderColor = colors.borderSubtle,
                                focusedContainerColor = colors.surfaceCanvas,
                                unfocusedContainerColor = colors.surfaceCanvas
                            )
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.surfaceElevated)
                                .hairlineBorder(colors.borderSubtle, RoundedCornerShape(8.dp))
                                .tactileClick {
                                    focusManager.clearFocus()
                                    viewModel.linkGitHubToken()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLinking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = colors.textPrimary
                                )
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Link,
                                        contentDescription = null,
                                        tint = colors.textPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Привязать токен GitHub",
                                        style = typography.caption.copy(
                                            color = colors.textPrimary,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // GitHub API Rate Limits Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surfacePanel)
                .hairlineBorder(colors.borderGhost, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Speed,
                        contentDescription = null,
                        tint = colors.signalCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Лимиты GitHub REST API",
                        style = typography.title.copy(fontSize = 14.sp, color = colors.textPrimary)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Осталось запросов:", style = typography.bodyMedium.copy(color = colors.textSecondary))
                    val quotaText = if (liveRateLimit != null) {
                        "${liveRateLimit?.remaining} / ${liveRateLimit?.limit}"
                    } else {
                        "$rateLimitRemaining / " + (if (authProvider == AuthProvider.GITHUB || !linkedGitHubToken.isNullOrBlank()) "5000" else "60")
                    }
                    Text(
                        text = quotaText,
                        style = typography.monoData.copy(color = colors.textPrimary)
                    )
                }

                if (rateLimitResetTime.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Сброс квоты:", style = typography.bodyMedium.copy(color = colors.textSecondary))
                        Text(
                            text = rateLimitResetTime,
                            style = typography.monoData.copy(color = colors.textSecondary)
                        )
                    }
                }
            }
        }

        // Release Notifications Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surfacePanel)
                .hairlineBorder(colors.borderGhost, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = null,
                        tint = colors.signalGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Уведомления о релизах",
                            style = typography.title.copy(fontSize = 14.sp, color = colors.textPrimary)
                        )
                        Text(
                            text = "Проверка обновлений избранных репозиториев каждые 12ч",
                            style = typography.caption.copy(color = colors.textTertiary, fontSize = 11.sp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = isNotificationsEnabled,
                    onCheckedChange = { viewModel.toggleReleaseNotifications(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colors.textInverse,
                        checkedTrackColor = colors.textPrimary,
                        uncheckedThumbColor = colors.textSecondary,
                        uncheckedTrackColor = colors.surfaceSubtle
                    )
                )
            }
        }

        // Presets Backup (Export / Import JSON) Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surfacePanel)
                .hairlineBorder(colors.borderGhost, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.FileDownload,
                        contentDescription = null,
                        tint = colors.signalCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Резервное копирование пресетов",
                        style = typography.title.copy(fontSize = 14.sp, color = colors.textPrimary)
                    )
                }

                Text(
                    text = "Экспорт и импорт списка пользовательских фильтров через буфер обмена в формате JSON.",
                    style = typography.bodyMedium.copy(color = colors.textSecondary, fontSize = 12.sp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Export Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.surfaceElevated)
                            .hairlineBorder(colors.borderSubtle, RoundedCornerShape(8.dp))
                            .tactileClick {
                                coroutineScope.launch {
                                    val json = viewModel.exportPresetsJson()
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    clipboard?.setPrimaryClip(ClipData.newPlainText("GitNews Presets", json))
                                    Toast.makeText(context, "Пресеты экспортированы в буфер обмена", Toast.LENGTH_SHORT).show()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.FileDownload,
                                contentDescription = null,
                                tint = colors.textPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Экспорт",
                                style = typography.caption.copy(color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }

                    // Import Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.surfaceElevated)
                            .hairlineBorder(colors.borderSubtle, RoundedCornerShape(8.dp))
                            .tactileClick {
                                coroutineScope.launch {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    val clipText = clipboard?.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                                    if (clipText.isBlank()) {
                                        Toast.makeText(context, "Буфер обмена пуст", Toast.LENGTH_SHORT).show()
                                        return@launch
                                    }
                                    val result = viewModel.importPresetsJson(clipText)
                                    result.onSuccess { count ->
                                        Toast.makeText(context, "Импортировано $count пресетов", Toast.LENGTH_SHORT).show()
                                    }.onFailure {
                                        Toast.makeText(context, "Ошибка импорта: неверный JSON", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.FileUpload,
                                contentDescription = null,
                                tint = colors.textPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Импорт",
                                style = typography.caption.copy(color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }
            }
        }

        // Logout Action Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surfacePanel)
                .hairlineBorder(colors.signalCrimson, RoundedCornerShape(8.dp))
                .tactileClick { showLogoutDialog = true },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Logout,
                    contentDescription = null,
                    tint = colors.signalCrimson,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Выйти из аккаунта",
                    style = typography.caption.copy(
                        color = colors.signalCrimson,
                        fontSize = 13.sp
                    )
                )
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Выйти из аккаунта?", color = colors.textPrimary) },
            text = { Text("Текущая сессия будет завершена, а данные входа удалены из защищенного хранилища.", color = colors.textSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.logout()
                        showLogoutDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.signalCrimson)
                ) {
                    Text("Выйти", color = colors.textPrimary)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showLogoutDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceSubtle)
                ) {
                    Text("Отмена", color = colors.textSecondary)
                }
            },
            containerColor = colors.surfacePanel
        )
    }
}
