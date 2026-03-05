package com.clupics.ui.screens

import android.app.Activity
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.clupics.R
import com.clupics.data.Album
import com.clupics.ui.GaleriaUiState
import com.clupics.ui.GaleriaViewModel
import com.clupics.utils.GaleriaImageLoader

// Orden para álbumes
enum class AlbumSortOrder { DATE_DESC, DATE_ASC, NAME_ASC, NAME_DESC }
private fun List<Album>.applySort(order: AlbumSortOrder) = when (order) {
    AlbumSortOrder.DATE_DESC  -> sortedByDescending { it.id }
    AlbumSortOrder.DATE_ASC   -> sortedBy          { it.id }
    AlbumSortOrder.NAME_ASC   -> sortedBy          { it.name.lowercase() }
    AlbumSortOrder.NAME_DESC  -> sortedByDescending { it.name.lowercase() }
}

private val E_DEC = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
private val E_ACC = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalFoundationApi::class)
@Composable
fun AlbumsScreen(
    uiState        : GaleriaUiState,
    viewModel      : GaleriaViewModel,
    onAlbumClick   : (Long) -> Unit
) {
    val context        = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    var selectedIds      by remember { mutableStateOf(emptySet<Long>()) }
    var selectingMode    by remember { mutableStateOf(false) }
    val isSelecting       = selectingMode || selectedIds.isNotEmpty()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var sortOrder        by rememberSaveable { mutableStateOf(AlbumSortOrder.NAME_ASC) }

    val sortedAlbums = remember(uiState.albums, sortOrder) { uiState.albums.applySort(sortOrder) }

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { r ->
        if (r.resultCode == Activity.RESULT_OK) {
            selectedIds = emptySet(); selectingMode = false; viewModel.loadMedia()
        }
    }

    fun deleteSelected() {
        val uris = selectedIds.flatMap { viewModel.getMediaUrisForAlbum(it) }
        if (uris.isEmpty()) { selectedIds = emptySet(); selectingMode = false; return }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            deleteLauncher.launch(IntentSenderRequest.Builder(
                MediaStore.createDeleteRequest(context.contentResolver, uris)).build())
        else { uris.forEach { context.contentResolver.delete(it, null, null) }
               selectedIds = emptySet(); selectingMode = false; viewModel.loadMedia() }
    }

    // El gesto predictivo nativo lo maneja el sistema directamente
    // gracias a enableOnBackInvokedCallback=true en el manifest.
    // Solo interceptamos BackHandler cuando hay selección activa,
    // para cancelarla en lugar de salir de la pantalla.
    androidx.activity.compose.BackHandler(enabled = isSelecting) {
        selectedIds   = emptySet()
        selectingMode = false
    }

    val sortGroup = MenuDropdownGroup(
        icon  = Icons.AutoMirrored.Filled.Sort,
        label = stringResource(R.string.sort_label),
        items = listOf(
            MenuDropdownItem(Icons.AutoMirrored.Filled.Sort, stringResource(R.string.sort_date_desc),
                isChecked = sortOrder == AlbumSortOrder.DATE_DESC, keepSheetOpen = true) { sortOrder = AlbumSortOrder.DATE_DESC },
            MenuDropdownItem(Icons.AutoMirrored.Filled.Sort, stringResource(R.string.sort_date_asc),
                isChecked = sortOrder == AlbumSortOrder.DATE_ASC,  keepSheetOpen = true) { sortOrder = AlbumSortOrder.DATE_ASC  },
            MenuDropdownItem(Icons.Filled.SortByAlpha,       stringResource(R.string.sort_name_asc),
                isChecked = sortOrder == AlbumSortOrder.NAME_ASC,  keepSheetOpen = true) { sortOrder = AlbumSortOrder.NAME_ASC  },
            MenuDropdownItem(Icons.Filled.SortByAlpha,       stringResource(R.string.sort_name_desc),
                isChecked = sortOrder == AlbumSortOrder.NAME_DESC, keepSheetOpen = true) { sortOrder = AlbumSortOrder.NAME_DESC }
        )
    )
    val menuEntries: List<MenuEntry> = buildList {
        if (!isSelecting) {
            add(MenuEntry.Action(MenuDropdownItem(
                Icons.Outlined.Circle, stringResource(R.string.selection_select_albums)
            ) { selectingMode = true }))
        } else if (selectedIds.isNotEmpty()) {
            add(MenuEntry.Action(MenuDropdownItem(
                Icons.Filled.DeleteForever, stringResource(R.string.selection_delete),
                isDestructive = true
            ) { showDeleteDialog = true }))
        }
        add(MenuEntry.Group(sortGroup))
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = isSelecting,
            transitionSpec = {
                val enter = fadeIn(tween(180, easing = E_DEC)) +
                        slideInVertically(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)) { if (targetState) -it else it }
                val exit  = fadeOut(tween(120, easing = E_ACC)) +
                        slideOutVertically(tween(140, easing = E_ACC)) { if (targetState) it else -it }
                enter togetherWith exit using SizeTransform(clip = false)
            },
            label = "albumsTopBar"
        ) { selecting ->
            if (selecting) {
                TopAppBar(
                    title = {
                        AnimatedContent(targetState = selectedIds.size, transitionSpec = {
                            val up = targetState > initialState
                            (fadeIn(tween(120)) + slideInVertically(tween(140)) { if (up) -it else it }) togetherWith
                                    (fadeOut(tween(100)) + slideOutVertically(tween(120)) { if (up) it else -it })
                        }, label = "albumSelCount") { count ->
                            val text = if (count == 0)
                                stringResource(R.string.selection_tap_to_select_albums)
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
                        val allSelected = selectedIds.size >= sortedAlbums.size && sortedAlbums.isNotEmpty()
                        IconButton(onClick = { selectedIds = if (allSelected) emptySet() else sortedAlbums.map { it.id }.toSet() }) {
                            Icon(
                                if (allSelected) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                                stringResource(R.string.selection_select_all)
                            )
                        }
                        MenuSplitButtonEntries(stringResource(R.string.menu_label), Icons.Filled.Menu, { },
                            menuEntries, Modifier.padding(end = 12.dp))
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface)
                )
            } else {
                MediumTopAppBar(
                    title          = { Text(stringResource(R.string.albums_title)) },
                    actions        = {
                        MenuSplitButtonEntries(stringResource(R.string.menu_label), Icons.Filled.Menu, { },
                            menuEntries, Modifier.padding(end = 12.dp))
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.mediumTopAppBarColors(
                        containerColor         = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface)
                )
            }
        }

        Crossfade(targetState = sortedAlbums.isEmpty() && !uiState.isLoading,
            animationSpec = tween(300), modifier = Modifier.fillMaxSize(), label = "albumsState") { empty ->
            if (empty) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.Collections, null, Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(16.dp))
                        Text(stringResource(R.string.albums_empty),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                LazyVerticalGrid(
                    columns               = GridCells.Fixed(2),
                    modifier              = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
                    contentPadding        = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 12.dp + navBarBottom + 80.dp),
                    verticalArrangement   = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(sortedAlbums, key = { it.id }) { album ->
                        val sel = album.id in selectedIds
                        AlbumCard(album, sel,
                            onClick = {
                                if (isSelecting) selectedIds = if (sel) selectedIds - album.id else selectedIds + album.id
                                else onAlbumClick(album.id)
                            },
                            onLongClick = { selectingMode = true; selectedIds = selectedIds + album.id }
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            shape  = MaterialTheme.shapes.extraLarge,
            title  = { Text(stringResource(R.string.selection_delete_confirm_title)) },
            text   = {
                val total = selectedIds.sumOf { id -> uiState.albums.firstOrNull { it.id == id }?.itemCount ?: 0 }
                Text(stringResource(R.string.selection_delete_confirm_msg, total))
            },
            confirmButton  = {
                TextButton(onClick = { showDeleteDialog = false; deleteSelected() }) {
                    Text(stringResource(R.string.selection_delete_confirm_ok),
                        color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton  = { TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.selection_delete_confirm_cancel)) } }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AlbumCard(album: Album, isSelected: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    val context      = LocalContext.current
    val imageLoader  = remember { GaleriaImageLoader.get(context) }
    // remember keyed on album.id — stable request = Coil never reloads on recomposition
    val thumbSizePx  = remember { GaleriaImageLoader.thumbSizePx(context, columns = 2) }
    val imageRequest = remember(album.id) {
        if (album.coverUri != null)
            GaleriaImageLoader.requestFor(context, album.id, album.coverUri, sizePx = thumbSizePx)
        else null
    }
    val cookieShape = MaterialShapes.Cookie9Sided.toShape()
    val cardColor   by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        tween(200), label = "cardColor"
    )
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape    = MaterialTheme.shapes.large,
        colors   = CardDefaults.elevatedCardColors(containerColor = cardColor)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).padding(10.dp)
                .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(0.2f) else MaterialTheme.colorScheme.primaryContainer, cookieShape)
                .clip(cookieShape)) {
                if (imageRequest != null)
                    AsyncImage(model = imageRequest,
                        imageLoader = imageLoader, contentDescription = album.name,
                        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else
                    Icon(Icons.Filled.Collections, null, Modifier.size(40.dp).align(Alignment.Center),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer)
                if (isSelected) {
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)))
                    Icon(Icons.Filled.CheckCircle, null, Modifier.align(Alignment.TopEnd).padding(8.dp).size(26.dp),
                        tint = MaterialTheme.colorScheme.primary)
                }
            }
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(album.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(stringResource(R.string.albums_items, album.itemCount),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
