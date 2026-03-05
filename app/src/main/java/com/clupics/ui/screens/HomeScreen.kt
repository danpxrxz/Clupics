package com.clupics.ui.screens

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.clupics.R
import com.clupics.data.MediaItem
import com.clupics.data.MediaType
import com.clupics.ui.GaleriaUiState
import com.clupics.utils.GaleriaImageLoader

// ─────────────────────────────────────────────────────────────────────────────
// Clasificación
// ─────────────────────────────────────────────────────────────────────────────
enum class SortOrder { DATE_DESC, DATE_ASC, NAME_ASC, NAME_DESC }

internal fun List<MediaItem>.applySort(order: SortOrder) = when (order) {
    SortOrder.DATE_DESC -> sortedByDescending { it.dateAdded }
    SortOrder.DATE_ASC  -> sortedBy          { it.dateAdded }
    SortOrder.NAME_ASC  -> sortedBy          { it.name.lowercase() }
    SortOrder.NAME_DESC -> sortedByDescending { it.name.lowercase() }
}

private enum class HomeState { LOADING, EMPTY, CONTENT }
private val E_DEC = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
private val E_ACC = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)

// ─────────────────────────────────────────────────────────────────────────────
// HomeScreen
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    uiState        : GaleriaUiState,
    gridColumns    : Int = 3,
    gridState      : androidx.compose.foundation.lazy.grid.LazyGridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState(),
    onRefresh      : () -> Unit,
    onMediaClick   : (Long, Rect?) -> Unit,
    onSortChanged  : ((SortOrder) -> Unit)? = null,
    onSettingsClick: () -> Unit = {}
) {
    val context        = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    // ── Selección ────────────────────────────────────────────────────────
    // selectingMode: usuario tocó "Seleccionar" pero aún no eligió ningún ítem
    var selectingMode    by remember { mutableStateOf(false) }
    var selectedIds      by remember { mutableStateOf(emptySet<Long>()) }
    val isSelecting       = selectingMode || selectedIds.isNotEmpty()
    var showDeleteDialog by remember { mutableStateOf(false) }

    // ── Orden ────────────────────────────────────────────────────────────
    var sortOrder by rememberSaveable { mutableStateOf(SortOrder.DATE_DESC) }
    val sortedMedia = remember(uiState.filteredMedia, sortOrder) {
        uiState.filteredMedia.applySort(sortOrder)
    }

    val screenState = when {
        uiState.isLoading && sortedMedia.isEmpty() -> HomeState.LOADING
        sortedMedia.isEmpty()                      -> HomeState.EMPTY
        else                                       -> HomeState.CONTENT
    }

    // ── Acciones ─────────────────────────────────────────────────────────
    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedIds = emptySet()
            selectingMode = false
            onRefresh()
        }
    }

    fun deleteSelected() {
        val uris = sortedMedia.filter { it.id in selectedIds }.map { it.uri }
        if (uris.isEmpty()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            deleteLauncher.launch(
                IntentSenderRequest.Builder(
                    MediaStore.createDeleteRequest(context.contentResolver, uris)
                ).build()
            )
        } else {
            uris.forEach { context.contentResolver.delete(it, null, null) }
            selectedIds = emptySet()
            selectingMode = false
            onRefresh()
        }
    }

    fun shareSelected() {
        val uris = sortedMedia.filter { it.id in selectedIds }.map { it.uri }
        if (uris.isEmpty()) return
        val intent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, uris[0])
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        context.startActivity(Intent.createChooser(intent, null))
    }

    BackHandler(enabled = isSelecting) {
        selectedIds = emptySet()
        selectingMode = false
    }

    // ── Entradas del SplitButton ──────────────────────────────────────────
    val sortGroup = MenuDropdownGroup(
        icon  = Icons.AutoMirrored.Filled.Sort,
        label = stringResource(R.string.sort_label),
        items = listOf(
            MenuDropdownItem(Icons.AutoMirrored.Filled.Sort, stringResource(R.string.sort_date_desc),
                isChecked = sortOrder == SortOrder.DATE_DESC, keepSheetOpen = true) { sortOrder = SortOrder.DATE_DESC; onSortChanged?.invoke(sortOrder) },
            MenuDropdownItem(Icons.AutoMirrored.Filled.Sort, stringResource(R.string.sort_date_asc),
                isChecked = sortOrder == SortOrder.DATE_ASC,  keepSheetOpen = true) { sortOrder = SortOrder.DATE_ASC; onSortChanged?.invoke(sortOrder) },
            MenuDropdownItem(Icons.Filled.SortByAlpha,       stringResource(R.string.sort_name_asc),
                isChecked = sortOrder == SortOrder.NAME_ASC,  keepSheetOpen = true) { sortOrder = SortOrder.NAME_ASC; onSortChanged?.invoke(sortOrder) },
            MenuDropdownItem(Icons.Filled.SortByAlpha,       stringResource(R.string.sort_name_desc),
                isChecked = sortOrder == SortOrder.NAME_DESC, keepSheetOpen = true) { sortOrder = SortOrder.NAME_DESC; onSortChanged?.invoke(sortOrder) }
        )
    )
    val menuEntries: List<MenuEntry> = buildList {
        if (!isSelecting) {
            add(MenuEntry.Action(MenuDropdownItem(
                Icons.Outlined.Circle, stringResource(R.string.selection_select)
            ) { selectingMode = true }))
        } else if (selectedIds.isNotEmpty()) {
            add(MenuEntry.Action(MenuDropdownItem(
                Icons.Filled.Share, stringResource(R.string.selection_share)
            ) { shareSelected() }))
            add(MenuEntry.Action(MenuDropdownItem(
                Icons.Filled.DeleteForever, stringResource(R.string.selection_delete),
                isDestructive = true
            ) { showDeleteDialog = true }))
        }
        add(MenuEntry.Group(sortGroup))
    }

    // ── UI ───────────────────────────────────────────────────────────────
    Column(modifier = Modifier.fillMaxSize()) {

        AnimatedContent(
            targetState = isSelecting,
            transitionSpec = {
                val enter = fadeIn(tween(180, easing = E_DEC)) +
                        slideInVertically(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)) { if (targetState) -it else it }
                val exit = fadeOut(tween(120, easing = E_ACC)) +
                        slideOutVertically(tween(140, easing = E_ACC)) { if (targetState) it else -it }
                enter togetherWith exit using SizeTransform(clip = false)
            },
            label = "homeTopBar"
        ) { selecting ->
            if (selecting) {
                TopAppBar(
                    title = {
                        AnimatedContent(
                            targetState = selectedIds.size,
                            transitionSpec = {
                                val up = targetState > initialState
                                (fadeIn(tween(120)) + slideInVertically(tween(140)) { if (up) -it else it }) togetherWith
                                        (fadeOut(tween(100)) + slideOutVertically(tween(120)) { if (up) it else -it })
                            },
                            label = "selCount"
                        ) { count ->
                            val text = if (count == 0)
                                stringResource(R.string.selection_tap_to_select)
                            else
                                stringResource(R.string.selection_count, count)
                            Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { selectedIds = emptySet(); selectingMode = false }) {
                            Icon(Icons.Filled.Close, stringResource(R.string.selection_cancel_desc))
                        }
                    },
                    actions = {
                        // Select-all button — visible en modo selección
                        val allSelected = selectedIds.size >= sortedMedia.size && sortedMedia.isNotEmpty()
                        IconButton(onClick = { selectedIds = if (allSelected) emptySet() else sortedMedia.map { it.id }.toSet() }) {
                            Icon(
                                if (allSelected) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                                stringResource(R.string.selection_select_all)
                            )
                        }
                        MenuSplitButtonEntries(
                            menuLabel = stringResource(R.string.menu_settings),
                            menuIcon  = Icons.Outlined.Settings,
                            onLeading = { onSettingsClick() },
                            entries   = menuEntries,
                            modifier  = Modifier.padding(end = 12.dp)
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            } else {
                LargeTopAppBar(
                    title          = { Text(stringResource(R.string.home_title)) },
                    scrollBehavior = scrollBehavior,
                    actions = {
                        MenuSplitButtonEntries(
                            menuLabel = stringResource(R.string.menu_settings),
                            menuIcon  = Icons.Outlined.Settings,
                            onLeading = { onSettingsClick() },
                            entries   = menuEntries,
                            modifier  = Modifier.padding(end = 12.dp)
                        )
                    },
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor         = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        }

        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh    = onRefresh,
            modifier     = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection)
        ) {
            Crossfade(
                targetState   = screenState,
                animationSpec = tween(320),
                modifier      = Modifier.fillMaxSize(),
                label         = "homeState"
            ) { state ->
                when (state) {
                    HomeState.LOADING -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    HomeState.EMPTY   -> HomeEmptyState()
                    HomeState.CONTENT -> MediaGrid(
                        mediaItems       = sortedMedia,
                        columns          = gridColumns,
                        selectedIds      = selectedIds,
                        isSelecting      = isSelecting,
                        gridState        = gridState,
                        onMediaClick     = { id, rect ->
                            if (isSelecting) {
                                selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
                            } else {
                                onMediaClick(id, rect)
                            }
                        },
                        onMediaLongClick = { id ->
                            selectingMode = true
                            selectedIds = selectedIds + id
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            shape  = MaterialTheme.shapes.extraLarge,
            title  = { Text(stringResource(R.string.selection_delete_confirm_title)) },
            text   = { Text(stringResource(R.string.selection_delete_confirm_msg, selectedIds.size)) },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; deleteSelected() }) {
                    Text(
                        stringResource(R.string.selection_delete_confirm_ok),
                        color      = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.selection_delete_confirm_cancel))
                }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MediaGrid
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaGrid(
    mediaItems      : List<MediaItem>,
    columns         : Int = 3,
    selectedIds     : Set<Long> = emptySet(),
    isSelecting     : Boolean = false,
    gridState       : androidx.compose.foundation.lazy.grid.LazyGridState = rememberLazyGridState(),
    onMediaClick    : (Long, Rect?) -> Unit,
    onMediaLongClick: (Long) -> Unit = {},
    modifier        : Modifier = Modifier
) {
    val context     = LocalContext.current
    // Singleton estable — no se recrea entre recomposiciones
    val imageLoader = remember { GaleriaImageLoader.get(context) }
    // Tamaño de miniatura calculado una vez para toda la grilla según la densidad real
    val thumbSizePx = remember(columns) { GaleriaImageLoader.thumbSizePx(context, columns) }

    // navBarBottom: altura real de la navbar del sistema + espacio extra para
    // que el último elemento no quede tapado por la navbar de la galería.
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LazyVerticalGrid(
        columns               = GridCells.Fixed(columns),
        state                 = gridState,
        modifier              = modifier.fillMaxSize(),
        contentPadding        = PaddingValues(start = 2.dp, top = 2.dp, end = 2.dp, bottom = 2.dp + navBarBottom + 80.dp),
        verticalArrangement   = Arrangement.spacedBy(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        // flingBehavior por defecto — NO sobreescribir para conservar el scroll nativo
    ) {
        items(mediaItems, key = { it.id }) { item ->
            // selectedIds y isSelecting se pasan como lambda para evitar que un cambio
            // en la selección de UN ítem cause recomposición de TODOS los ítems de la grilla.
            val selected = item.id in selectedIds
            MediaGridItem(
                item        = item,
                isSelected  = selected,
                isSelecting = isSelecting,
                imageLoader = imageLoader,
                thumbSizePx = thumbSizePx,
                onClick     = { rect -> onMediaClick(item.id, rect) },
                onLongClick = { onMediaLongClick(item.id) }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MediaGridItem
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MediaGridItem(
    item       : MediaItem,
    isSelected : Boolean = false,
    isSelecting: Boolean = false,
    imageLoader: coil.ImageLoader,
    thumbSizePx: Int = 256,
    onClick    : (Rect?) -> Unit,
    onLongClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scale by animateFloatAsState(
        targetValue   = if (isSelected) 0.88f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label         = "itemScale"
    )
    val overlayAlpha by animateFloatAsState(
        targetValue   = if (isSelected) 0.20f else 0f,
        animationSpec = tween(160),
        label         = "itemOverlay"
    )
    val itemShape = if (isSelected) MaterialShapes.Cookie9Sided.toShape()
                   else MaterialTheme.shapes.extraSmall
    // Request estable por id — Coil no recrea el objeto si el id no cambia
    val imageRequest = remember(item.id) {
        GaleriaImageLoader.requestFor(context, item.id, item.uri, sizePx = thumbSizePx)
    }

    var itemBounds by remember { mutableStateOf<Rect?>(null) }

    Box(modifier = Modifier
        .aspectRatio(1f)
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .onGloballyPositioned { coords -> itemBounds = coords.boundsInWindow() }
    ) {
        Box(modifier = Modifier
            .fillMaxSize()
            .clip(itemShape)
            .combinedClickable(onClick = { onClick(itemBounds) }, onLongClick = onLongClick)
        ) {
            AsyncImage(
                model              = imageRequest,
                imageLoader        = imageLoader,
                contentDescription = null,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier.fillMaxSize(),
                placeholder        = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                error              = ColorPainter(MaterialTheme.colorScheme.surfaceVariant)
            )
            if (item.mediaType == MediaType.VIDEO) {
                Box(Modifier.fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.25f)))
                Icon(
                    Icons.Filled.PlayCircle, null,
                    Modifier.align(Alignment.Center).size(32.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            if (overlayAlpha > 0f)
                Box(Modifier.fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = overlayAlpha)))
        }

        // Checkmark de selección
        androidx.compose.animation.AnimatedVisibility(
            visible  = isSelecting,
            enter    = fadeIn(tween(80)) + scaleIn(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium), 0.4f),
            exit     = fadeOut(tween(80)) + scaleOut(tween(80), 0.4f),
            modifier = Modifier.align(Alignment.TopStart).padding(5.dp)
        ) {
            AnimatedContent(
                targetState = isSelected,
                transitionSpec = {
                    (fadeIn(tween(100)) + scaleIn(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium), 0.5f)) togetherWith
                            (fadeOut(tween(70)) + scaleOut(tween(70), 0.5f))
                },
                label = "itemCheck"
            ) { sel ->
                if (sel) {
                    Icon(
                        Icons.Filled.CheckCircle, null,
                        tint     = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Box(Modifier.size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.70f))
                    ) {
                        Icon(
                            Icons.Outlined.Circle, null,
                            tint     = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.fillMaxSize().padding(1.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Estado vacío
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun HomeEmptyState() {
    Column(
        modifier            = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Outlined.PhotoLibrary, null, Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.home_empty_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(R.string.home_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline)
    }
}
