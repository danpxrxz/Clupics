package com.clupics.ui

import android.os.Build
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.clupics.R
import com.clupics.ui.screens.AlbumDetailScreen
import com.clupics.ui.screens.AlbumsScreen
import com.clupics.ui.screens.HomeScreen
import com.clupics.ui.screens.MediaViewerScreen
import com.clupics.ui.screens.PermissionScreen
import com.clupics.ui.screens.SettingsScreen
import com.clupics.ui.settings.SettingsViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Home        : Screen("home")
    object Albums      : Screen("albums")
    object Settings    : Screen("settings")
    object AlbumDetail : Screen("album/{albumId}") {
        fun createRoute(albumId: Long) = "album/$albumId"
    }
    object MediaViewer : Screen("viewer/{mediaId}/{albumId}") {
        fun createRoute(mediaId: Long, albumId: Long = -1L) = "viewer/$mediaId/$albumId"
    }
}

private val bottomNavItems = listOf(Screen.Home, Screen.Albums)
private fun String?.isFullscreen() = this?.startsWith("viewer/") == true
private fun String?.hideNavBar()   = isFullscreen() == true || this == "settings"

private val E_DEC = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
private val E_ACC = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)

// ── Animaciones de pestañas principales (Home ↔ Albums) ──────────────────────
// Fade simple — el scale causaba recomposición del Scaffold y la navbar aparecía
// de golpe en cada cambio de pestaña.
private val tabEnter: EnterTransition = fadeIn(tween(220, easing = E_DEC))
private val tabExit:  ExitTransition  = fadeOut(tween(160, easing = E_ACC))

// ── Animaciones de pantallas "push" (AlbumDetail, Settings) ──────────────────
// Slide horizontal estilo iOS/Material3: entra desde la derecha, sale hacia la
// izquierda. Al hacer back (pop) se invierte. Combinado con fade para suavidad.
// NOTA: EnterTransition.None en estas pantallas desactivaría la animación nativa
// del gesto predictivo de tarjeta — por eso usamos transiciones propias.
// El gesto predictivo sigue funcionando porque enableOnBackInvokedCallback=true
// en el manifest hace que Android intercepte el gesto ANTES de que Compose lo vea.
private val pushDuration = 350

private val pushEnter: EnterTransition =
    slideInHorizontally(tween(pushDuration, easing = E_DEC)) { it } +
    fadeIn(tween(pushDuration / 2, easing = E_DEC))

private val pushExit: ExitTransition =
    slideOutHorizontally(tween(pushDuration, easing = E_DEC)) { -it / 3 } +
    fadeOut(tween(pushDuration / 2, easing = E_ACC))

private val popEnter: EnterTransition =
    slideInHorizontally(tween(pushDuration, easing = E_DEC)) { -it / 3 } +
    fadeIn(tween(pushDuration / 2, easing = E_DEC))

private val popExit: ExitTransition =
    slideOutHorizontally(tween(pushDuration, easing = E_DEC)) { it } +
    fadeOut(tween(pushDuration / 2, easing = E_ACC))

// ── Animaciones del Viewer (fotos/videos) ─────────────────────────────────────
// Fade + slide vertical suave: entra desde abajo (sensación de "elevar" la foto),
// cierra volviendo abajo. Duración más larga para sentirse fluido y elegante.
private val viewerDuration = 380

private val viewerEnter: EnterTransition =
    slideInVertically(tween(viewerDuration, easing = E_DEC)) { it / 6 } +
    fadeIn(tween(viewerDuration * 2 / 3, easing = E_DEC))

private val viewerExit: ExitTransition =
    slideOutVertically(tween(viewerDuration, easing = E_ACC)) { it / 6 } +
    fadeOut(tween(viewerDuration / 2, easing = E_ACC))

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun GaleriaApp(settingsViewModel: SettingsViewModel) {
    val vm            : GaleriaViewModel = viewModel()
    val uiState       by vm.uiState.collectAsStateWithLifecycle()
    val settings      by settingsViewModel.state.collectAsStateWithLifecycle()
    val openThumbRect by vm.openThumbRect.collectAsStateWithLifecycle()

    LaunchedEffect(settings.showImages, settings.showVideos) {
        vm.applyFilters(settings.showImages, settings.showVideos)
    }

    val mediaPerms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        listOf(android.Manifest.permission.READ_MEDIA_IMAGES, android.Manifest.permission.READ_MEDIA_VIDEO)
    else listOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)

    val permState = rememberMultiplePermissionsState(
        permissions         = mediaPerms,
        onPermissionsResult = { p -> if (p.values.any { it }) vm.onPermissionGranted() }
    )

    if (permState.permissions.none { it.status.isGranted }) {
        PermissionScreen { permState.launchMultiplePermissionRequest() }
        return
    }
    LaunchedEffect(uiState.hasPermission) { if (!uiState.hasPermission) vm.onPermissionGranted() }

    val nav            = rememberNavController()
    val backStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute    = backStackEntry?.destination?.route
    val hideNav         = currentRoute.hideNavBar()

    val homeGridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()

    fun isAlbumsActive(r: String?) = r == Screen.Albums.route || r?.startsWith("album/") == true

    Scaffold(
        modifier            = Modifier.fillMaxSize(),
        // contentWindowInsets=0 → inner padding siempre cero.
        // El viewer ocupa toda la pantalla sin restricciones del Scaffold.
        // Home y Albums añaden navigationBarsPadding() en sus propios grids.
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (!hideNav) {
                NavigationBar(
                    // Color opaco del tema — la navbar de la galería debe ser visible
                    // y distinta del contenido, no transparente.
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 0.dp
                ) {
                    val dest = backStackEntry?.destination
                    bottomNavItems.forEach { screen ->
                        val sel = when (screen) {
                            Screen.Albums -> isAlbumsActive(currentRoute)
                            else -> dest?.hierarchy?.any { it.route == screen.route } == true
                        }
                        val label = when (screen) {
                            Screen.Home   -> stringResource(R.string.nav_home)
                            Screen.Albums -> stringResource(R.string.nav_folders)
                            else -> ""
                        }
                        NavigationBarItem(
                            selected = sel,
                            label    = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            icon     = {
                                when (screen) {
                                    Screen.Home   -> Icon(if (sel) Icons.Filled.Image  else Icons.Outlined.Image,      label)
                                    Screen.Albums -> Icon(if (sel) Icons.Filled.Folder else Icons.Outlined.FolderOpen, label)
                                    else -> {}
                                }
                            },
                            onClick  = {
                                nav.navigate(screen.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true; restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { _ ->
        NavHost(
            navController      = nav,
            startDestination   = Screen.Home.route,
            modifier           = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
            enterTransition    = { tabEnter },
            exitTransition     = { tabExit },
            popEnterTransition = { tabEnter },
            popExitTransition  = { tabExit }
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    uiState         = uiState,
                    gridColumns     = settings.gridColumns,
                    gridState       = homeGridState,
                    onRefresh       = { vm.loadMedia() },
                    onSortChanged   = { order ->
                        vm.setHomeSortOrder(order)
                        scope.launch { homeGridState.scrollToItem(0) }
                    },
                    onMediaClick    = { mediaId, rect ->
                        vm.setOpenThumbRect(rect?.let {
                            GaleriaViewModel.ThumbRect(it.left, it.top, it.width, it.height)
                        })
                        nav.navigate(Screen.MediaViewer.createRoute(mediaId, 0L))
                    },
                    onSettingsClick = { nav.navigate(Screen.Settings.route) }
                )
            }
            composable(Screen.Albums.route) {
                AlbumsScreen(
                    uiState      = uiState,
                    viewModel    = vm,
                    onAlbumClick = { nav.navigate(Screen.AlbumDetail.createRoute(it)) }
                )
            }
            composable(
                route              = Screen.AlbumDetail.route,
                arguments          = listOf(navArgument("albumId") { type = NavType.LongType }),
                enterTransition    = { pushEnter },
                exitTransition     = { pushExit },
                popEnterTransition = { popEnter  },
                popExitTransition  = { popExit   }
            ) { back ->
                val albumId = back.arguments?.getLong("albumId") ?: return@composable
                val album   = uiState.albums.firstOrNull { it.id == albumId } ?: return@composable
                AlbumDetailScreen(
                    album        = album,
                    mediaItems   = vm.getMediaForAlbum(albumId),
                    onBack       = { nav.popBackStack() },
                    onMediaClick = { mediaId, rect ->
                        vm.setOpenThumbRect(rect?.let {
                            GaleriaViewModel.ThumbRect(it.left, it.top, it.width, it.height)
                        })
                        nav.navigate(Screen.MediaViewer.createRoute(mediaId, albumId))
                    }
                )
            }
            composable(
                route              = Screen.Settings.route,
                enterTransition    = { pushEnter },
                exitTransition     = { pushExit  },
                popEnterTransition = { popEnter  },
                popExitTransition  = { popExit   }
            ) {
                SettingsScreen(settingsViewModel, onBack = { nav.popBackStack() })
            }
            composable(
                route              = Screen.MediaViewer.route,
                arguments          = listOf(
                    navArgument("mediaId") { type = NavType.LongType },
                    navArgument("albumId") { type = NavType.LongType; defaultValue = -1L }
                ),
                enterTransition    = { viewerEnter },
                exitTransition     = { viewerExit  },
                popEnterTransition = { viewerEnter },
                popExitTransition  = { viewerExit  }
            ) { back ->
                val mediaId  = back.arguments?.getLong("mediaId") ?: return@composable
                val albumId  = back.arguments?.getLong("albumId") ?: -1L
                val item     = vm.getMediaItem(mediaId) ?: return@composable
                val allItems = when {
                    albumId == 0L -> vm.getSortedHomeMedia()
                    albumId > 0L  -> vm.getMediaForAlbum(albumId)
                    else          -> null
                }
                MediaViewerScreen(
                    mediaItem    = item,
                    allItems     = allItems,
                    initialIndex = allItems?.indexOfFirst { it.id == mediaId }?.coerceAtLeast(0) ?: 0,
                    openFromRect = openThumbRect,
                    onBack       = {
                        vm.setOpenThumbRect(null)
                        nav.popBackStack()
                    }
                )
            }
        }
    }
}

