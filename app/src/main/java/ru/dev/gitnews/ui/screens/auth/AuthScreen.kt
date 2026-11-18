package ru.dev.gitnews.ui.screens.auth

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.dev.gitnews.domain.model.AuthState
import ru.dev.gitnews.ui.theme.GitNewsTheme
import ru.dev.gitnews.ui.theme.hairlineBorder
import ru.dev.gitnews.ui.theme.tactileClick
import java.util.ArrayList

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onAuthSuccess: () -> Unit
) {
    val colors = GitNewsTheme.colors
    val typography = GitNewsTheme.typography
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val authState by viewModel.authState.collectAsState()
    val isPolling by viewModel.isPolling.collectAsState()
    val patInput by viewModel.patInput.collectAsState()
    val emailInput by viewModel.emailInput.collectAsState()
    val passwordInput by viewModel.passwordInput.collectAsState()
    val displayNameInput by viewModel.displayNameInput.collectAsState()
    val isRegisterMode by viewModel.isRegisterMode.collectAsState()
    val isPasswordVisible by viewModel.isPasswordVisible.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val resetMessage by viewModel.resetMessage.collectAsState()

    var isCodeCopied by remember { mutableStateOf(false) }
    var showPatInput by remember { mutableStateOf(false) }
    var showGitHubSection by remember { mutableStateOf(false) }

    if (authState is AuthState.Authenticated) {
        onAuthSuccess()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surfaceCanvas)
            .statusBarsPadding()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {

            // Minimalist Brand Heading
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "GitNews",
                    style = typography.display.copy(
                        fontSize = 34.sp,
                        color = colors.textPrimary,
                        letterSpacing = (-0.03).sp
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Единый хаб open-source трендов и аналитики",
                    style = typography.bodyMedium.copy(
                        color = colors.textSecondary,
                        fontSize = 13.sp
                    )
                )
            }

            // Error Banner
            if (!errorMessage.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.surfaceSubtle)
                        .hairlineBorder(colors.signalCrimson, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        style = typography.caption.copy(color = colors.signalCrimson)
                    )
                }
            }

            // Password Reset Confirmation Banner
            AnimatedVisibility(visible = !resetMessage.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.surfaceElevated)
                        .hairlineBorder(colors.signalEmerald.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
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
                            text = resetMessage ?: "",
                            style = typography.caption.copy(
                                color = colors.textPrimary,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }

            // PROVIDER 1: Google 1-Tap Sign-In
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(9999.dp))
                    .background(colors.surfaceElevated)
                    .hairlineBorder(colors.borderSubtle, RoundedCornerShape(9999.dp))
                    .tactileClick {
                        viewModel.clearError()
                        viewModel.clearResetMessage()
                        viewModel.signInWithGoogle(context)
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    GoogleGlyph(modifier = Modifier.size(18.dp), tint = colors.textPrimary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Войти через Google",
                        style = typography.caption.copy(
                            color = colors.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    )
                }
            }

            // Divider: OR EMAIL
            SectionDivider(text = "ИЛИ ПО EMAIL")

            // PROVIDER 2: Email + Password Authentication Panel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surfacePanel)
                    .hairlineBorder(colors.borderGhost, RoundedCornerShape(12.dp))
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

                    // Segmented Toggle: Вход | Регистрация
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(9999.dp))
                            .background(colors.surfaceCanvas)
                            .hairlineBorder(colors.borderGhost, RoundedCornerShape(9999.dp))
                            .padding(3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .clip(RoundedCornerShape(9999.dp))
                                .background(if (!isRegisterMode) colors.surfaceElevated else Color.Transparent)
                                .hairlineBorder(
                                    if (!isRegisterMode) colors.borderSubtle else Color.Transparent,
                                    RoundedCornerShape(9999.dp)
                                )
                                .tactileClick { if (isRegisterMode) viewModel.toggleRegisterMode() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Вход",
                                style = typography.caption.copy(
                                    color = if (!isRegisterMode) colors.textPrimary else colors.textTertiary,
                                    fontWeight = if (!isRegisterMode) FontWeight.SemiBold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .clip(RoundedCornerShape(9999.dp))
                                .background(if (isRegisterMode) colors.surfaceElevated else Color.Transparent)
                                .hairlineBorder(
                                    if (isRegisterMode) colors.borderSubtle else Color.Transparent,
                                    RoundedCornerShape(9999.dp)
                                )
                                .tactileClick { if (!isRegisterMode) viewModel.toggleRegisterMode() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Регистрация",
                                style = typography.caption.copy(
                                    color = if (isRegisterMode) colors.textPrimary else colors.textTertiary,
                                    fontWeight = if (isRegisterMode) FontWeight.SemiBold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }

                    // Display Name (Only in Registration Mode)
                    AnimatedVisibility(visible = isRegisterMode) {
                        OutlinedTextField(
                            value = displayNameInput,
                            onValueChange = viewModel::onDisplayNameChanged,
                            placeholder = { Text("Ваше имя или никнейм", color = colors.textTertiary, fontSize = 13.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
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
                                focusedContainerColor = colors.surfaceCanvas,
                                unfocusedContainerColor = colors.surfaceCanvas
                            )
                        )
                    }

                    // Email Field
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = viewModel::onEmailChanged,
                        placeholder = { Text("email@example.com", color = colors.textTertiary, fontSize = 13.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Email,
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
                            focusedContainerColor = colors.surfaceCanvas,
                            unfocusedContainerColor = colors.surfaceCanvas
                        )
                    )

                    // Password Field
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = viewModel::onPasswordChanged,
                        placeholder = { Text("Пароль (минимум 6 символов)", color = colors.textTertiary, fontSize = 13.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus()
                            viewModel.authenticateWithEmail()
                        }),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = colors.textTertiary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = viewModel::togglePasswordVisibility) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = null,
                                    tint = colors.textTertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
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

                    // "Забыли пароль?" text button in login mode
                    if (!isRegisterMode) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "Забыли пароль?",
                                style = typography.caption.copy(
                                    color = colors.textSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier
                                    .tactileClick {
                                        focusManager.clearFocus()
                                        viewModel.sendPasswordReset()
                                    }
                                    .padding(vertical = 2.dp)
                            )
                        }
                    }

                    // Action Button (Вход / Регистрация)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.textPrimary)
                            .tactileClick {
                                focusManager.clearFocus()
                                viewModel.authenticateWithEmail()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = colors.textInverse
                            )
                        } else {
                            Text(
                                text = if (isRegisterMode) "Зарегистрироваться" else "Войти в аккаунт",
                                style = typography.caption.copy(
                                    color = colors.textInverse,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            )
                        }
                    }
                }
            }

            // Divider: OR GITHUB
            SectionDivider(text = "ИЛИ ЧЕРЕЗ GITHUB")

            // PROVIDER 3: GitHub (Device Flow / PAT)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surfacePanel)
                    .hairlineBorder(colors.borderGhost, RoundedCornerShape(12.dp))
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .tactileClick { showGitHubSection = !showGitHubSection },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Code,
                                contentDescription = null,
                                tint = colors.textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "GitHub OAuth / Токен",
                                style = typography.title.copy(
                                    fontSize = 14.sp,
                                    color = colors.textPrimary
                                )
                            )
                        }
                        Text(
                            text = if (showGitHubSection) "Скрыть" else "Развернуть",
                            style = typography.caption.copy(
                                color = colors.textSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }

                    if (showGitHubSection) {
                        Text(
                            text = "Прямой вход через аккаунт GitHub для синхронизации звезд и лимитов 5000 req/hr",
                            style = typography.caption.copy(
                                fontSize = 11.sp,
                                color = colors.textTertiary
                            )
                        )

                        when (val state = authState) {
                            is AuthState.Authenticating -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.surfaceCanvas)
                                        .hairlineBorder(colors.borderStrong, RoundedCornerShape(8.dp))
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = state.userCode,
                                        style = typography.monoData.copy(
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.textPrimary,
                                            letterSpacing = 2.sp
                                        )
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(42.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.textPrimary)
                                        .tactileClick {
                                            try {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                                clipboard?.setPrimaryClip(ClipData.newPlainText("GitHub Code", state.userCode))
                                                isCodeCopied = true

                                                val customTabs = CustomTabsIntent.Builder().build()
                                                customTabs.launchUrl(context, Uri.parse(state.verificationUri))
                                            } catch (_: Exception) {}
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (isCodeCopied) Icons.Outlined.Check else Icons.Outlined.OpenInBrowser,
                                            contentDescription = null,
                                            tint = colors.textInverse,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isCodeCopied) "Код скопирован! Открыть сайт" else "Скопировать код и открыть GitHub",
                                            style = typography.caption.copy(
                                                color = colors.textInverse,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 12.sp
                                            )
                                        )
                                    }
                                }

                                if (isPolling) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(12.dp),
                                            strokeWidth = 2.dp,
                                            color = colors.signalEmerald
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Ожидание подтверждения в браузере...",
                                            style = typography.caption.copy(
                                                fontSize = 11.sp,
                                                color = colors.textSecondary
                                            )
                                        )
                                    }
                                }
                            }

                            else -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(42.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.surfaceElevated)
                                        .hairlineBorder(colors.borderSubtle, RoundedCornerShape(8.dp))
                                        .tactileClick {
                                            viewModel.startDeviceFlow()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Получить одноразовый код (Device Flow)",
                                        style = typography.caption.copy(
                                            color = colors.textPrimary,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp
                                        )
                                    )
                                }
                            }
                        }

                        // PAT toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .tactileClick { showPatInput = !showPatInput }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Key,
                                contentDescription = null,
                                tint = colors.textTertiary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (showPatInput) "Скрыть ввод токена PAT" else "Использовать личный токен (PAT)...",
                                style = typography.caption.copy(
                                    color = colors.textSecondary,
                                    fontSize = 11.sp
                                )
                            )
                        }

                        AnimatedVisibility(visible = showPatInput) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = patInput,
                                    onValueChange = viewModel::onPatInputChanged,
                                    placeholder = { Text("ghp_xxxxxxxxxxxxxxxxxxxx", color = colors.textTertiary, fontSize = 12.sp) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = {
                                        focusManager.clearFocus()
                                        viewModel.authenticateWithPat()
                                    }),
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
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.surfaceCanvas)
                                        .hairlineBorder(colors.borderSubtle, RoundedCornerShape(8.dp))
                                        .tactileClick {
                                            focusManager.clearFocus()
                                            viewModel.authenticateWithPat()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Войти по токену PAT",
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
    }
}

@Composable
private fun SectionDivider(
    text: String
) {
    val colors = GitNewsTheme.colors
    val typography = GitNewsTheme.typography
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(colors.borderGhost)
        )
        Text(
            text = text,
            style = typography.caption.copy(
                color = colors.textTertiary,
                fontSize = 10.sp,
                letterSpacing = 1.sp
            ),
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(colors.borderGhost)
        )
    }
}

@Composable
fun GoogleGlyph(
    modifier: Modifier = Modifier,
    tint: Color
) {
    Canvas(modifier = modifier) {
        val sizePx = size.minDimension
        val strokeWidth = sizePx * 0.16f
        val radius = (sizePx - strokeWidth) / 2f
        val centerOffset = center

        drawArc(
            color = tint,
            startAngle = 45f,
            sweepAngle = 275f,
            useCenter = false,
            topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        drawLine(
            color = tint,
            start = Offset(centerOffset.x, centerOffset.y),
            end = Offset(centerOffset.x + radius, centerOffset.y),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}
