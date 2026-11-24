package me.animeheaven.android.ui.navigation

import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import me.animeheaven.android.AppContainer
import me.animeheaven.android.data.runCatchingCancellable
import me.animeheaven.android.ui.components.AhLoadingBox
import me.animeheaven.android.ui.components.MobileBottomBar
import me.animeheaven.android.ui.components.MobileNavigationRail
import me.animeheaven.android.ui.components.TopTabDestination
import me.animeheaven.android.ui.screens.bookmarks.BookmarksScreen
import me.animeheaven.android.ui.screens.bookmarks.BookmarksViewModel
import me.animeheaven.android.ui.screens.catalog.CatalogKind
import me.animeheaven.android.ui.screens.catalog.CatalogScreen
import me.animeheaven.android.ui.screens.catalog.CatalogViewModel
import me.animeheaven.android.ui.screens.detail.AnimeDetailScreen
import me.animeheaven.android.ui.screens.detail.DetailViewModel
import me.animeheaven.android.ui.screens.home.HomeScreen
import me.animeheaven.android.ui.screens.home.HomeViewModel
import me.animeheaven.android.ui.screens.player.PlayerScreen
import me.animeheaven.android.ui.screens.player.PlayerViewModel
import me.animeheaven.android.ui.screens.search.RepositorySearchGateway
import me.animeheaven.android.ui.screens.search.SearchScreen
import me.animeheaven.android.ui.screens.search.SearchViewModel
import me.animeheaven.android.ui.screens.settings.SettingsScreen

private object Routes {
    const val HOME = "home"
    const val NEW = "new"
    const val POPULAR = "popular"
    const val BOOKMARKS = "bookmarks"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val SEASONAL = "seasonal/{path}/{label}"
    const val TAG = "tag/{tag}"
    const val DETAIL = "detail/{animeId}"
    const val PLAYER = "player/{episodeKey}/{animeId}/{animeTitle}/{coverUrl}"

    fun seasonal(path: String, label: String) =
        "seasonal/${Uri.encode(path)}/${Uri.encode(label)}"

    fun tag(tag: String) = "tag/${Uri.encode(tag)}"

    fun detail(animeId: String) = "detail/${Uri.encode(animeId)}"

    fun player(
        episodeKey: String,
        animeId: String,
        animeTitle: String,
        coverUrl: String
    ): String = "player/${Uri.encode(episodeKey)}/${Uri.encode(animeId.ifBlank { "_" })}/" +
        "${Uri.encode(animeTitle.ifBlank { "AnimeHeaven" })}/${Uri.encode(coverUrl.ifBlank { "_" })}"
}

@Composable
fun AppNavGraph(
    container: AppContainer,
    isInPipModeFlow: StateFlow<Boolean>,
    onRegisterPipEligibility: ((() -> Boolean)?) -> Unit,
    onRequestPip: () -> Unit
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isInPipMode by isInPipModeFlow.collectAsStateWithLifecycle()
    val useNavigationRail = LocalConfiguration.current.screenWidthDp >= 600
    var resolvingRandom by remember { mutableStateOf(false) }

    val activeTopTab = when (currentRoute) {
        Routes.HOME -> TopTabDestination.HOME
        Routes.NEW -> TopTabDestination.NEW
        Routes.POPULAR -> TopTabDestination.POPULAR
        Routes.BOOKMARKS -> TopTabDestination.BOOKMARKS
        else -> null
    }

    val navigateTopTab: (String) -> Unit = remember(navController) {
        { route ->
            navController.navigate(route) {
                popUpTo(Routes.HOME) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    val openRandomAnime: () -> Unit = remember(navController, container, context) {
        {
            if (!resolvingRandom) {
                resolvingRandom = true
                scope.launch {
                    try {
                        runCatchingCancellable {
                            val id = container.repository.getRandomAnimeId()
                            check(id.isNotBlank())
                            navController.navigate(Routes.detail(id))
                        }.onFailure {
                            Toast.makeText(context, "Couldn’t pick a random anime. Try again.", Toast.LENGTH_SHORT).show()
                        }
                    } finally {
                        resolvingRandom = false
                    }
                }
            }
        }
    }

    val openAnime: (String) -> Unit = remember(navController) {
        { id -> if (id.isNotBlank()) navController.navigate(Routes.detail(id)) }
    }
    val openSearch: () -> Unit = remember(navController) { { navController.navigate(Routes.SEARCH) } }
    val openPopular: () -> Unit = remember(navigateTopTab) { { navigateTopTab(Routes.POPULAR) } }
    val openSeasonal: (String, String) -> Unit = remember(navController) {
        { path, label -> if (path.isNotBlank()) navController.navigate(Routes.seasonal(path, label.ifBlank { "Season" })) }
    }
    val openSettings: () -> Unit = remember(navController) { { navController.navigate(Routes.SETTINGS) } }
    val popBack: () -> Unit = remember(navController) { { navController.popBackStack() } }
    val selectTopTab: (TopTabDestination) -> Unit = { tab ->
        navigateTopTab(when (tab) {
            TopTabDestination.HOME -> Routes.HOME
            TopTabDestination.NEW -> Routes.NEW
            TopTabDestination.POPULAR -> Routes.POPULAR
            TopTabDestination.BOOKMARKS -> Routes.BOOKMARKS
        })
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            if (useNavigationRail && activeTopTab != null && !isInPipMode) {
                MobileNavigationRail(activeTopTab, selectTopTab)
            }
            Column(modifier = Modifier.weight(1f)) {
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                enterTransition = { fadeIn(animationSpec = tween(140)) },
                exitTransition = { fadeOut(animationSpec = tween(110)) },
                popEnterTransition = { fadeIn(animationSpec = tween(140)) },
                popExitTransition = { fadeOut(animationSpec = tween(110)) }
            ) {
                composable(Routes.HOME) {
                    val vm: HomeViewModel = viewModel(
                        factory = simpleFactory {
                            HomeViewModel(
                                container.repository,
                                container.settingsStore,
                                container.preloader
                            )
                        }
                    )
                    HomeScreen(
                        viewModel = vm,
                        onOpenAnime = openAnime,
                        onPlayContinueItem = { cw ->
                            if (cw.targetEpisodeKey.isNotBlank())
                            navController.navigate(
                                Routes.player(
                                    episodeKey = cw.targetEpisodeKey,
                                    animeId = cw.animeId,
                                    animeTitle = cw.animeTitle,
                                    coverUrl = cw.coverUrl
                                )
                            )
                        },
                        onOpenSearch = openSearch,
                        onOpenPopular = openPopular,
                        onOpenSeasonal = openSeasonal,
                        onOpenRandom = openRandomAnime,
                        onOpenSettings = openSettings
                    )
                }

                composable(Routes.NEW) {
                    val vm: CatalogViewModel = viewModel(
                        factory = simpleFactory {
                            CatalogViewModel(
                                container.repository,
                                container.settingsStore,
                                CatalogKind.NEW,
                                "new.php",
                                "New Episodes",
                                container.preloader
                            )
                        }
                    )
                    CatalogScreen(
                        viewModel = vm,
                        onOpenAnime = openAnime,
                        onOpenSearch = openSearch,
                        onOpenSeasonal = openSeasonal,
                        onOpenRandom = openRandomAnime,
                        onOpenSettings = openSettings
                    )
                }

                composable(Routes.POPULAR) {
                    val vm: CatalogViewModel = viewModel(
                        factory = simpleFactory {
                            CatalogViewModel(
                                container.repository,
                                container.settingsStore,
                                CatalogKind.POPULAR,
                                "popular.php",
                                "Popular Today",
                                container.preloader
                            )
                        }
                    )
                    CatalogScreen(
                        viewModel = vm,
                        onOpenAnime = openAnime,
                        onOpenSearch = openSearch,
                        onOpenSeasonal = openSeasonal,
                        onOpenRandom = openRandomAnime,
                        onOpenSettings = openSettings
                    )
                }

                composable(Routes.BOOKMARKS) {
                    val vm: BookmarksViewModel = viewModel(
                        factory = simpleFactory {
                            BookmarksViewModel(container.repository, container.preloader)
                        }
                    )
                    BookmarksScreen(
                        viewModel = vm,
                        onOpenHome = { navigateTopTab(Routes.HOME) },
                        onOpenAnime = openAnime,
                        onPlayContinueItem = { cw ->
                            if (cw.targetEpisodeKey.isNotBlank())
                            navController.navigate(
                                Routes.player(
                                    episodeKey = cw.targetEpisodeKey,
                                    animeId = cw.animeId,
                                    animeTitle = cw.animeTitle,
                                    coverUrl = cw.coverUrl
                                )
                            )
                        },
                        onOpenSearch = openSearch,
                        onOpenSeasonal = openSeasonal,
                        onOpenRandom = openRandomAnime,
                        onOpenSettings = openSettings
                    )
                }

                composable(
                    route = Routes.SEASONAL,
                    arguments = listOf(
                        navArgument("path") { type = NavType.StringType },
                        navArgument("label") { type = NavType.StringType }
                    )
                ) { entry ->
                    val path = entry.arguments?.getString("path").orEmpty()
                    val label = entry.arguments?.getString("label").orEmpty()
                    val vm: CatalogViewModel = viewModel(
                        factory = simpleFactory {
                            CatalogViewModel(
                                container.repository,
                                container.settingsStore,
                                CatalogKind.SEASONAL,
                                path,
                                "$label Anime",
                                container.preloader
                            )
                        }
                    )
                    CatalogScreen(
                        viewModel = vm,
                        onOpenAnime = openAnime,
                        onOpenSearch = openSearch,
                        onOpenSeasonal = openSeasonal,
                        onOpenRandom = openRandomAnime,
                        onOpenSettings = openSettings,
                        onBack = popBack
                    )
                }

                composable(
                    route = Routes.TAG,
                    arguments = listOf(navArgument("tag") { type = NavType.StringType })
                ) { entry ->
                    val tag = entry.arguments?.getString("tag").orEmpty()
                    val vm: CatalogViewModel = viewModel(
                        factory = simpleFactory {
                            CatalogViewModel(
                                container.repository,
                                container.settingsStore,
                                CatalogKind.TAG,
                                tag,
                                "$tag Anime",
                                container.preloader
                            )
                        }
                    )
                    CatalogScreen(
                        viewModel = vm,
                        onOpenAnime = openAnime,
                        onOpenSearch = openSearch,
                        onOpenSeasonal = openSeasonal,
                        onOpenRandom = openRandomAnime,
                        onOpenSettings = openSettings,
                        onBack = popBack
                    )
                }

                composable(Routes.SEARCH) {
                    val vm: SearchViewModel = viewModel(
                        factory = simpleFactory {
                            SearchViewModel(
                                gateway = RepositorySearchGateway(container.repository),
                                preloader = container.preloader
                            )
                        }
                    )
                    SearchScreen(
                        viewModel = vm,
                        onBack = popBack,
                        onOpenAnime = openAnime
                    )
                }

                composable(
                    route = Routes.DETAIL,
                    arguments = listOf(navArgument("animeId") { type = NavType.StringType })
                ) { entry ->
                    val animeId = entry.arguments?.getString("animeId").orEmpty()
                    val vm: DetailViewModel = viewModel(
                        factory = simpleFactory {
                            DetailViewModel(container.repository, animeId, container.preloader)
                        }
                    )
                    AnimeDetailScreen(
                        viewModel = vm,
                        onBack = popBack,
                        onPlayEpisode = { key, id, title, cover ->
                            if (key.isNotBlank()) navController.navigate(Routes.player(key, id, title, cover))
                        },
                        onOpenTag = { tag -> if (tag.isNotBlank()) navController.navigate(Routes.tag(tag)) },
                        onOpenAnime = openAnime
                    )
                }

                composable(
                    route = Routes.PLAYER,
                    arguments = listOf(
                        navArgument("episodeKey") { type = NavType.StringType },
                        navArgument("animeId") { type = NavType.StringType },
                        navArgument("animeTitle") { type = NavType.StringType },
                        navArgument("coverUrl") { type = NavType.StringType }
                    )
                ) { entry ->
                    val epKey = entry.arguments?.getString("episodeKey").orEmpty()
                    val animeId = entry.arguments?.getString("animeId").orEmpty()
                        .let { if (it == "_") "" else it }
                    val animeTitle = entry.arguments?.getString("animeTitle").orEmpty()
                    val coverUrl = entry.arguments?.getString("coverUrl").orEmpty()
                        .let { if (it == "_") "" else it }

                    val vm: PlayerViewModel = viewModel(
                        factory = simpleFactory {
                            PlayerViewModel(
                                repository = container.repository,
                                settingsStore = container.settingsStore,
                                skipTimesClient = container.skipTimesClient,
                                initialEpisodeKey = epKey,
                                initialAnimeId = animeId,
                                initialAnimeTitle = animeTitle,
                                initialCoverUrl = coverUrl,
                                progressScope = container.appScope,
                                videoCacheManager = container.videoCacheManager,
                                preloader = container.preloader
                            )
                        }
                    )
                    PlayerScreen(
                        viewModel = vm,
                        videoCacheManager = container.videoCacheManager,
                        isInPipMode = isInPipMode,
                        onRegisterPipEligibility = onRegisterPipEligibility,
                        onRequestPip = onRequestPip,
                        onBack = popBack
                    )
                }

                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        settingsStore = container.settingsStore,
                        repository = container.repository,
                        onGetCacheSummary = { container.getCacheSummaryText() },
                        onClearAllCaches = { container.clearAllCaches() },
                        onBack = popBack
                    )
                }
            }

                if (!useNavigationRail && activeTopTab != null && !isInPipMode) {
                    MobileBottomBar(activeTopTab, selectTopTab)
                }
            }
        }

        if (resolvingRandom) {
            Dialog(
                onDismissRequest = {},
                properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false,
                    usePlatformDefaultWidth = false)
            ) {
                Box(Modifier.fillMaxSize().background(Color(0xAA000000))) {
                    AhLoadingBox(message = "Picking a random anime...")
                }
            }
        }
    }
}

private inline fun <reified VM : ViewModel> simpleFactory(
    crossinline creator: () -> VM
): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = creator() as T
}
