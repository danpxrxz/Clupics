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
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.geometry.Rect
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clupics.R
import com.clupics.data.Album
import com.clupics.data.MediaItem

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AlbumDetailScreen(
    album       : Album,
    mediaItems  : List<MediaItem>,
    onBack      : () -> Unit,
    onMediaClick: (Long, androidx.compose.ui.geometry.Rect?) -> Unit
) {
    val context        = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    var selectedIds      by remember { mutableStateOf(emptySet<Long>()) }
    var selectingMode    by remember { mutableStateOf(false) }
    val isSelecting       = selectingMode || selectedIds.isNotEmpty()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var sortOrder        by rememberSaveable { mutableStateOf(SortOrder.DATE_DESC) }

    val sortedItems = remember(mediaItems, sortOrder) { mediaItems.applySort(sortOrder) }

    // El gesto predictivo nativo lo maneja el sistema directamente
    // gracias a enableOnBackInvokedCallback=true en el manifest.
    // Solo interceptamos cuando hay selección activa para cancelarla.
    androidx.activity.compose.BackHandler(enabled = isSelecting) {
        selectedIds   = emptySet()
        selectingMode = false
    }

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { r -> if (r.resultCode == Activity.RESULT_OK) { selectedIds = emptySet(); selectingMode = false; onBack() } }

    fun deleteSelected() {
        val uris = sortedItems.filter { it.id in selectedIds }.map { it.uri }
        if (uris.isEmpty()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            deleteLauncher.launch(IntentSenderRequest.Builder(
                MediaStore.createDeleteRequest(context.contentResolver, uris)).build())
        else { uris.forEach { context.contentResolver.delete(it, null, null) }
               selectedIds = emptySet(); selectingMode = false; onBack() }
    }

    fun shareSelected() {
        val uris = sortedItems.filter { it.id in selectedIds }.map { it.uri }
        if (uris.isEmpty()) return
        val intent = if (uris.size == 1)
            Intent(Intent.ACTION_SEND).apply {
                type = "image/*"; putExtra(Intent.EXTRA_STREAM, uris[0])
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        else Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/*"; putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, null))
    }

    val sortGroup = MenuDropdownGroup(
        icon  = Icons.AutoMirrored.Filled.Sort,
        label = stringResource(R.string.sort_label),
        items = listOf(
            MenuDropdownItem(Icons.AutoMirrored.Filled.Sort, stringResource(R.string.sort_date_desc),
                isChecked = sortOrder == SortOrder.DATE_DESC, keepSheetOpen = true) { sortOrder = SortOrder.DATE_DESC },
            MenuDropdownItem(Icons.AutoMirrored.Filled.Sort, stringResource(R.string.sort_date_asc),
                isChecked = sortOrder == SortOrder.DATE_ASC,  keepSheetOpen = true) { sortOrder = SortOrder.DATE_ASC  },
            MenuDropdownItem(Icons.Filled.SortByAlpha,       stringResource(R.string.sort_name_asc),
                isChecked = sortOrder == SortOrder.NAME_ASC,  keepSheetOpen = true) { sortOrder = SortOrder.NAME_ASC  },
            MenuDropdownItem(Icons.Filled.SortByAlpha,       stringResource(R.string.sort_name_desc),
                isChecked = sortOrder == SortOrder.NAME_DESC, keepSheetOpen = true) { sortOrder = SortOrder.NAME_DESC }
        )
    )
    val menuEntries: List<MenuEntry> = buildList {
        if (!isSelecting) {
            add(MenuEntry.Action(MenuDropdownItem(
                Icons.Outlined.Circle, stringResource(R.string.selection_select)
            ) { selectingMode = true }))
        } else if (selectedIds.isNotEmpty()) {
            add(MenuEntry.Action(MenuDropdownItem(Icons.Filled.Share, stringResource(R.string.selection_share)) { shareSelected() }))
            add(MenuEntry.Action(MenuDropdownItem(
                Icons.Filled.DeleteForever, stringResource(R.string.selection_delete), isDestructive = true
            ) { showDeleteDialog = true }))
        }
        add(MenuEntry.Group(sortGroup))
    }

    // CRÍTICO: No envolver en Surface(fillMaxSize) — bloquea el gesto predictivo.
    // Column con background explícito: fondo opaco normalmente, pero Android puede
    // aplicar el efecto de tarjeta predictiva correctamente.
    Column(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.surface)) {
        AnimatedContent(
            targetState = isSelecting,
            transitionSpec = {
                val enter = fadeIn(tween(180)) + slideInVertically(tween(200)) { if (targetState) -it else it }
                val exit  = fadeOut(tween(120)) + slideOutVertically(tween(140)) { if (targetState) it else -it }
                enter togetherWith exit using SizeTransform(clip = false)
            },
            label = "detailTopBar"
        ) { selecting ->
            if (selecting) {
                TopAppBar(
                    title = {
                        AnimatedContent(targetState = selectedIds.size, transitionSpec = {
                            val up = targetState > initialState
                            (fadeIn(tween(120)) + slideInVertically(tween(140)) { if (up) -it else it }) togetherWith
                                    (fadeOut(tween(100)) + slideOutVertically(tween(120)) { if (up) it else -it })
                        }, label = "detailSelCount") { count ->
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
                        val allSelected = selectedIds.size >= sortedItems.size && sortedItems.isNotEmpty()
                        IconButton(onClick = { selectedIds = if (allSelected) emptySet() else sortedItems.map { it.id }.toSet() }) {
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
                LargeTopAppBar(
                    title = {
                        Column {
                            Text(album.name, style = MaterialTheme.typography.headlineMedium)
                            Text(stringResource(R.string.album_item_count, sortedItems.size), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.viewer_back))
                        }
                    },
                    actions = {
                        MenuSplitButtonEntries(stringResource(R.string.menu_label), Icons.Filled.Menu, { },
                            menuEntries, Modifier.padding(end = 12.dp))
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor         = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface)
                )
            }
        }

        MediaGrid(
            mediaItems       = sortedItems,
            columns          = 3,
            selectedIds      = selectedIds,
            isSelecting      = isSelecting,
            onMediaClick     = { id, rect ->
                if (isSelecting) selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
                else onMediaClick(id, rect)
            },
            onMediaLongClick = { id -> selectingMode = true; selectedIds = selectedIds + id },
            modifier         = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection)
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            shape  = MaterialTheme.shapes.extraLarge,
            title  = { Text(stringResource(R.string.selection_delete_confirm_title)) },
            text   = { Text(stringResource(R.string.selection_delete_confirm_msg, selectedIds.size)) },
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
