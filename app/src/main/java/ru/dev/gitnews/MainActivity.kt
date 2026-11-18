package ru.dev.gitnews

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.dev.gitnews.domain.model.AuthState
import ru.dev.gitnews.domain.model.RepositoryItem
import ru.dev.gitnews.ui.components.BottomNavBar
import ru.dev.gitnews.ui.components.NavigationTab
import ru.dev.gitnews.ui.components.TopBar
import ru.dev.gitnews.ui.components.YandexBannerAd
import ru.dev.gitnews.ui.screens.auth.AuthScreen
import ru.dev.gitnews.ui.screens.auth.AuthViewModel
import ru.dev.gitnews.ui.screens.bookmarks.BookmarksScreen
import ru.dev.gitnews.ui.screens.bookmarks.BookmarksViewModel
import ru.dev.gitnews.ui.screens.detail.RepoDetailScreen
import ru.dev.gitnews.ui.screens.detail.RepoDetailViewModel
import ru.dev.gitnews.ui.screens.feed.FeedScreen
import ru.dev.gitnews.ui.screens.feed.FeedViewModel
import ru.dev.gitnews.ui.screens.profile.ProfileScreen
import ru.dev.gitnews.ui.screens.profile.ProfileViewModel
import ru.dev.gitnews.ui.screens.search.SearchScreen
import ru.dev.gitnews.ui.screens.search.SearchViewModel
import ru.dev.gitnews.ui.theme.GitNewsTheme

class MainActivity : ComponentActivity() {

    private var initialNotificationRepo by mutableStateOf<Pair<String, String>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        handleNotificationIntent(intent)

        val app = application as GitNewsApplication
        val container = app.container

        setContent {
            val systemDark = isSystemInDarkTheme()
            var isDarkTheme by remember { mutableStateOf(systemDark) }

            GitNewsTheme(darkTheme = isDarkTheme) {
                val authViewModel = remember {
                    AuthViewModel(container.authRepository)
                }
                val authState by authViewModel.authState.collectAsState()

                when (authState) {
                    is AuthState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(GitNewsTheme.colors.surfaceCanvas)
                        )
                    }
                    is AuthState.Authenticated -> {
                        MainAppScaffold(
                            app = app,
                            isDarkTheme = isDarkTheme,
                            onToggleTheme = { isDarkTheme = !isDarkTheme },
                            initialNotificationRepo = initialNotificationRepo
                        )
                    }
                    else -> {
                        AuthScreen(
                            viewModel = authViewModel,
                            onAuthSuccess = {}
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val owner = intent?.getStringExtra("repo_owner")
        val repo = intent?.getStringExtra("repo_name")
        if (!owner.isNullOrBlank() && !repo.isNullOrBlank()) {
            initialNotificationRepo = owner to repo
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            val app = application as? GitNewsApplication
            app?.let { applicationInstance ->
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        applicationInstance.container.gitNewsDatabase.translationCacheDao().clearAll()
                    } catch (_: Exception) {}
                    try {
                        applicationInstance.cacheDir.listFiles()?.forEach { file ->
                            if (file.name != "crash_log.txt") {
                                file.deleteRecursively()
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
        }
    }
}

@Composable
fun MainAppScaffold(
    app: GitNewsApplication,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    initialNotificationRepo: Pair<String, String>? = null
) {
    val container = app.container
    val colors = GitNewsTheme.colors
    val coroutineScope = rememberCoroutineScope()

    var currentTab by remember { mutableStateOf(NavigationTab.FEED) }
    var selectedRepoForDetail by remember { mutableStateOf<RepositoryItem?>(null) }
    val feedListState = rememberLazyListState()

    LaunchedEffect(initialNotificationRepo) {
        if (initialNotificationRepo != null) {
            val (owner, repo) = initialNotificationRepo
            selectedRepoForDetail = RepositoryItem(
                id = (owner.hashCode().toLong() shl 32) or (repo.hashCode().toLong() and 0xFFFFFFFFL),
                name = repo,
                fullName = "$owner/$repo",
                ownerLogin = owner,
                ownerAvatarUrl = "https://github.com/$owner.png",
                htmlUrl = "https://github.com/$owner/$repo",
                description = null,
                starsCount = 0,
                forksCount = 0,
                language = null,
                languageColor = null,
                licenseName = null,
                pushedAt = null,
                createdAt = null,
                topics = emptyList()
            )
        }
    }

    val feedViewModel = remember {
        FeedViewModel(
            gitHubRepository = container.gitHubRepository,
            presetRepository = container.presetRepository,
            bookmarkRepository = container.bookmarkRepository,
            translationManager = container.translationManager
        )
    }

    val searchViewModel = remember {
        SearchViewModel(
            gitHubRepository = container.gitHubRepository,
            presetRepository = container.presetRepository,
            bookmarkRepository = container.bookmarkRepository,
            searchHistoryDao = container.gitNewsDatabase.searchHistoryDao()
        )
    }

    val bookmarksViewModel = remember {
        BookmarksViewModel(
            bookmarkRepository = container.bookmarkRepository,
            gitHubRepository = container.gitHubRepository
        )
    }

    val profileViewModel = remember {
        ProfileViewModel(
            authRepository = container.authRepository,
            gitHubRepository = container.gitHubRepository,
            tokenStorage = container.secureTokenStorage,
            presetRepository = container.presetRepository,
            releaseTrackerRepository = container.releaseTrackerRepository
        )
    }

    val translationMode by feedViewModel.translationMode.collectAsState()

    // If detail screen is open, display RepoDetailScreen
    val detailItem = selectedRepoForDetail
    if (detailItem != null) {
        BackHandler {
            selectedRepoForDetail = null
        }
        val detailViewModel = remember(detailItem.id) {
            RepoDetailViewModel(
                rawOwner = detailItem.ownerLogin,
                rawRepo = detailItem.name,
                gitHubRepository = container.gitHubRepository,
                bookmarkRepository = container.bookmarkRepository,
                translationManager = container.translationManager
            )
        }
        RepoDetailScreen(
            viewModel = detailViewModel,
            onBackClick = { selectedRepoForDetail = null },
            onTopicClick = { topic ->
                selectedRepoForDetail = null
                currentTab = NavigationTab.SEARCH
                searchViewModel.onQueryChanged(topic)
                searchViewModel.performSearch()
            }
        )
    } else {
        Scaffold(
            topBar = {
                TopBar(
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = onToggleTheme,
                    translationMode = translationMode,
                    onSelectTranslationMode = feedViewModel::setTranslationMode,
                    onAppIconClick = {
                        coroutineScope.launch {
                            feedListState.animateScrollToItem(0)
                        }
                    }
                )
            },
            bottomBar = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    YandexBannerAd()
                    BottomNavBar(
                        currentTab = currentTab,
                        onTabSelected = { currentTab = it },
                        onReselectTab = { tab ->
                            if (tab == NavigationTab.FEED) {
                                coroutineScope.launch {
                                    feedListState.animateScrollToItem(0)
                                }
                            }
                        }
                    )
                }
            },
            containerColor = colors.surfaceCanvas
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(colors.surfaceCanvas)
            ) {
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "tabAnimation"
                ) { tab ->
                    when (tab) {
                        NavigationTab.FEED -> FeedScreen(
                            viewModel = feedViewModel,
                            listState = feedListState,
                            onNavigateToDetail = { selectedRepoForDetail = it }
                        )

                        NavigationTab.SEARCH -> SearchScreen(
                            viewModel = searchViewModel,
                            onNavigateToDetail = { selectedRepoForDetail = it }
                        )

                        NavigationTab.BOOKMARKS -> BookmarksScreen(
                            viewModel = bookmarksViewModel,
                            onNavigateToDetail = { selectedRepoForDetail = it }
                        )

                        NavigationTab.PROFILE -> ProfileScreen(
                            viewModel = profileViewModel
                        )
                    }
                }
            }
        }
    }
}
