package com.clupics.ui.screens

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import com.clupics.R
import com.clupics.cast.CastButton
import com.clupics.data.MediaItem
import com.clupics.data.MediaType
import com.clupics.ui.GaleriaViewModel
import com.clupics.utils.GaleriaImageLoader
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private const val MIN_SCALE = 1f
private const val MAX_SCALE = 8f
private val ZOOM_SNAP = spring<Float>(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow)
private val PAN_SNAP  = spring<Float>(Spring.DampingRatioNoBouncy,     Spring.StiffnessMediumLow)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaViewerScreen(
    mediaItem    : MediaItem,
    allItems     : List<MediaItem>? = null,
    initialIndex : Int = 0,
    openFromRect : GaleriaViewModel.ThumbRect? = null,
    onBack       : () -> Unit
) {
    val context = LocalContext.current
    val view    = LocalView.current

    val items       = allItems ?: listOf(mediaItem)
    val pagerState  = rememberPagerState(initialPage = initialIndex) { items.size }
    val currentItem = items.getOrElse(pagerState.currentPage) { mediaItem }
    val isVideo     = currentItem.mediaType == MediaType.VIDEO

    var isZoomed         by remember { mutableStateOf(false) }
    var showBars         by remember { mutableStateOf(true) }
    var showMenu         by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showInfoSheet    by remember { mutableStateOf(false) }

    // Animación de entrada suave (fade-in del contenido).
    // También controla la velocidad del gesto predictivo de back — un valor más
    // alto hace que la animación de salida se sienta más fluida y menos abrupta.
    var entered by remember { mutableStateOf(false) }
    val contentAlpha by animateFloatAsState(
        targetValue   = if (entered) 1f else 0f,
        animationSpec = tween(320),
        label         = "viewerEnterFade"
    )
    LaunchedEffect(Unit) { entered = true }
    LaunchedEffect(pagerState.currentPage) { isZoomed = false }

    // ── Barras del sistema ────────────────────────────────────────────────────
    // Las barras son SIEMPRE transparentes (configurado en MainActivity y themes.xml).
    // En el visor solo cambiamos el color de los iconos a blanco para que sean
    // visibles sobre el fondo negro. El Box negro de Compose se dibuja a pantalla
    // completa y se ve a través de las barras transparentes.
    // Al salir restauramos el color de iconos al estado previo (gestionado por GaleriaTheme).
    val window = remember { (context as Activity).window }
    val insetsCtrl = remember(view) {
        WindowCompat.getInsetsController(window, view).also {
            it.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
        }
    }

    DisposableEffect(Unit) {
        val prevLightStatus = insetsCtrl.isAppearanceLightStatusBars
        val prevLightNav    = insetsCtrl.isAppearanceLightNavigationBars

        // Solo iconos blancos — NO tocamos statusBarColor ni navigationBarColor.
        // Las barras permanecen transparentes; el negro del Box se ve detrás.
        insetsCtrl.isAppearanceLightStatusBars     = false
        insetsCtrl.isAppearanceLightNavigationBars = false
        insetsCtrl.show(WindowInsetsCompat.Type.systemBars())

        onDispose {
            // Restaurar color de iconos al estado anterior (claro u oscuro según tema)
            insetsCtrl.isAppearanceLightStatusBars     = prevLightStatus
            insetsCtrl.isAppearanceLightNavigationBars = prevLightNav
            insetsCtrl.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    LaunchedEffect(showBars) {
        if (showBars) insetsCtrl.show(WindowInsetsCompat.Type.systemBars())
        else          insetsCtrl.hide(WindowInsetsCompat.Type.systemBars())
    }

    // ── Gesto predictivo nativo ───────────────────────────────────────────────
    // Solo interceptamos el caso especial: zoom activo → hacer zoom-out
    // en lugar de salir. Sin zoom, el sistema maneja el gesto completo.
    val currentIsZoomed by rememberUpdatedState(isZoomed)

    androidx.activity.compose.PredictiveBackHandler(enabled = currentIsZoomed) { progress ->
        try {
            progress.collect { }
            isZoomed = false
        } catch (_: kotlinx.coroutines.CancellationException) { }
    }

    fun doClose() {
        insetsCtrl.show(WindowInsetsCompat.Type.systemBars())
        onBack()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    fun shareItem(item: MediaItem) {
        val i = Intent(Intent.ACTION_SEND).apply {
            type = item.mimeType.ifBlank { "image/*" }
            putExtra(Intent.EXTRA_STREAM, item.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(i, null))
    }

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { r ->
        if (r.resultCode == Activity.RESULT_OK) {
            insetsCtrl.show(WindowInsetsCompat.Type.systemBars())
            onBack()
        }
    }

    fun deleteItem(item: MediaItem) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            deleteLauncher.launch(
                IntentSenderRequest.Builder(
                    MediaStore.createDeleteRequest(context.contentResolver, listOf(item.uri))
                ).build()
            )
        } else {
            context.contentResolver.delete(item.uri, null, null)
            insetsCtrl.show(WindowInsetsCompat.Type.systemBars())
            onBack()
        }
    }

    // ── Layout ────────────────────────────────────────────────────────────────
    // El Box negro ocupa toda la pantalla. Las barras son transparentes, así que
    // el negro se ve a través de ellas → sin franja de color en ningún tema.
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = contentAlpha }
            .background(Color.Black)
    ) {
        HorizontalPager(
            state                   = pagerState,
            userScrollEnabled       = !isZoomed,
            beyondViewportPageCount = 1,
            pageSpacing             = 8.dp,
            key                     = { items[it].id },
            modifier                = Modifier.fillMaxSize()
        ) { page ->
            val pageItem = items[page]
            val isActive = page == pagerState.currentPage

            if (pageItem.mediaType == MediaType.VIDEO) {
                VideoPlayerScreen(
                    uri              = pageItem.uri,
                    title            = pageItem.name,
                    isActivePlayer   = isActive,
                    showControls     = showBars,
                    onToggleControls = { showBars = !showBars }
                )
            } else {
                ZoomableImage(
                    mediaItem     = pageItem,
                    onTap         = { showBars = !showBars },
                    onZoomChanged = { z -> isZoomed = z }
                )
            }
        }

        // ── Controles superiores ─────────────────────────────────────────────
        // Colores FIJOS: fondo negro semitransparente + iconos blancos.
        // El visor siempre tiene fondo negro → no dependemos del tema claro/oscuro.
        AnimatedVisibility(
            visible  = showBars,
            enter    = fadeIn(tween(120)),
            exit     = fadeOut(tween(80)),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { doClose() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            stringResource(R.string.viewer_back),
                            tint = Color.White
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isVideo) {
                        CastButton(modifier = Modifier.size(48.dp), tint = Color.White)
                    } else {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                Icons.Filled.MoreVert,
                                stringResource(R.string.viewer_options),
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }

    if (showMenu) {
        ModalBottomSheet(
            onDismissRequest = { showMenu = false },
            sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor   = MaterialTheme.colorScheme.surfaceContainerLow,
            shape            = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle       = { BottomSheetDefaults.DragHandle() },
            tonalElevation   = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth().navigationBarsPadding()
                    .padding(horizontal = 16.dp).padding(bottom = 24.dp, top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ExpressiveSheetCard(
                    Icons.Outlined.Share, stringResource(R.string.viewer_share),
                    MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.onSurface
                ) { showMenu = false; shareItem(currentItem) }
                ExpressiveSheetCard(
                    Icons.Outlined.Info, stringResource(R.string.viewer_info),
                    MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.onSurface
                ) { showMenu = false; showInfoSheet = true }
                ExpressiveSheetCard(
                    Icons.Filled.DeleteForever, stringResource(R.string.viewer_delete),
                    MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.error
                ) { showMenu = false; showDeleteDialog = true }
            }
        }
    }

    if (showInfoSheet) {
        ModalBottomSheet(
            onDismissRequest = { showInfoSheet = false },
            sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor   = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape            = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            MediaInfoContent(
                item     = currentItem,
                modifier = Modifier
                    .fillMaxWidth().navigationBarsPadding()
                    .padding(horizontal = 24.dp).padding(bottom = 32.dp)
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            shape   = MaterialTheme.shapes.extraLarge,
            title   = { Text(stringResource(R.string.viewer_delete_confirm_title)) },
            text    = { Text(stringResource(R.string.viewer_delete_confirm_msg)) },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; deleteItem(currentItem) }) {
                    Text(
                        stringResource(R.string.viewer_delete_confirm_ok),
                        color      = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.viewer_delete_confirm_cancel))
                }
            }
        )
    }
}

// ── ZoomableImage ─────────────────────────────────────────────────────────────
@Composable
fun ZoomableImage(
    mediaItem    : MediaItem,
    onTap        : () -> Unit,
    onZoomChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val loader  = remember { GaleriaImageLoader.get(context) }

    val scaleAnim = remember { Animatable(1f) }
    val panX      = remember { Animatable(0f) }
    val panY      = remember { Animatable(0f) }

    var boxSize       by remember { mutableStateOf(IntSize.Zero) }
    var intrinsicSize by remember { mutableStateOf(IntSize.Zero) }

    val verticalFillScale: Float = remember(boxSize, intrinsicSize) {
        if (intrinsicSize.width <= 0 || intrinsicSize.height <= 0 ||
            boxSize.width <= 0 || boxSize.height <= 0) return@remember 1f
        val imgA = intrinsicSize.width.toFloat() / intrinsicSize.height.toFloat()
        val boxA = boxSize.width.toFloat() / boxSize.height.toFloat()
        if (imgA > boxA) (imgA / boxA).coerceAtLeast(1f) else 1f
    }

    fun maxPanX(s: Float): Float {
        if (intrinsicSize.width <= 0 || boxSize.width <= 0)
            return (boxSize.width * (s - 1f) / 2f).coerceAtLeast(0f)
        val imgA = intrinsicSize.width.toFloat() / intrinsicSize.height
        val boxA = boxSize.width.toFloat() / boxSize.height
        val rW   = if (imgA > boxA) boxSize.width.toFloat() else boxSize.height * imgA
        return ((rW * s - boxSize.width) / 2f).coerceAtLeast(0f)
    }
    fun maxPanY(s: Float): Float {
        if (s < verticalFillScale - 0.001f) return 0f
        if (intrinsicSize.height <= 0 || boxSize.height <= 0)
            return (boxSize.height * (s - 1f) / 2f).coerceAtLeast(0f)
        val imgA = intrinsicSize.width.toFloat() / intrinsicSize.height
        val boxA = boxSize.width.toFloat() / boxSize.height
        val rH   = if (imgA > boxA) boxSize.width / imgA else boxSize.height.toFloat()
        return ((rH * s - boxSize.height) / 2f).coerceAtLeast(0f)
    }

    fun snapScale(target: Float) {
        val t = target.coerceIn(MIN_SCALE, MAX_SCALE)
        scope.launch { scaleAnim.animateTo(t, ZOOM_SNAP) }
        if (t <= 1f) {
            scope.launch { panX.animateTo(0f, PAN_SNAP) }
            scope.launch { panY.animateTo(0f, PAN_SNAP) }
            onZoomChanged(false)
        } else {
            scope.launch { panX.animateTo(panX.value.coerceIn(-maxPanX(t), maxPanX(t)), PAN_SNAP) }
            scope.launch { panY.animateTo(panY.value.coerceIn(-maxPanY(t), maxPanY(t)), PAN_SNAP) }
            onZoomChanged(true)
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { boxSize = it }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var mode = GestureMode.UNDECIDED
                    var totX = 0f
                    var totY = 0f
                    while (true) {
                        val ev     = awaitPointerEvent()
                        val active = ev.changes.filter { it.pressed }
                        if (active.isEmpty()) break
                        val zoom = ev.calculateZoom()
                        val pan  = ev.calculatePan()
                        if (mode == GestureMode.UNDECIDED) {
                            totX += pan.x; totY += pan.y
                            val moved = abs(totX) + abs(totY)
                            val pinch = abs(zoom - 1f) > 0.02f
                            when {
                                pinch || active.size >= 2           -> mode = GestureMode.ZOOM_PAN
                                moved > viewConfiguration.touchSlop -> mode =
                                    if (scaleAnim.value > 1.01f) GestureMode.ZOOM_PAN else GestureMode.PASS_THROUGH
                            }
                        }
                        if (mode == GestureMode.ZOOM_PAN) {
                            ev.changes.forEach { if (it.positionChanged()) it.consume() }
                            val s = (scaleAnim.value * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
                            scope.launch { scaleAnim.snapTo(s) }
                            if (s > 1f) {
                                scope.launch { panX.snapTo((panX.value + pan.x).coerceIn(-maxPanX(s), maxPanX(s))) }
                                if (s >= verticalFillScale - 0.001f)
                                    scope.launch { panY.snapTo((panY.value + pan.y).coerceIn(-maxPanY(s), maxPanY(s))) }
                                onZoomChanged(true)
                            } else {
                                scope.launch { panX.snapTo(0f) }
                                scope.launch { panY.snapTo(0f) }
                                onZoomChanged(false)
                            }
                        }
                    }
                    if (mode == GestureMode.ZOOM_PAN && scaleAnim.value < 1.1f)
                        snapScale(1f)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = { tap ->
                        if (scaleAnim.value > 1f) {
                            snapScale(1f)
                        } else {
                            val t  = 3f
                            val cx = boxSize.width / 2f
                            val cy = boxSize.height / 2f
                            val tx = ((cx - tap.x) * (t - 1f)).coerceIn(-maxPanX(t), maxPanX(t))
                            val ty = if (t >= verticalFillScale - 0.001f)
                                ((cy - tap.y) * (t - 1f)).coerceIn(-maxPanY(t), maxPanY(t)) else 0f
                            scope.launch { scaleAnim.animateTo(t, ZOOM_SNAP) }
                            scope.launch { panX.animateTo(tx, PAN_SNAP) }
                            scope.launch { panY.animateTo(ty, PAN_SNAP) }
                            onZoomChanged(true)
                        }
                    }
                )
            }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(mediaItem.uri)
                .size(Size.ORIGINAL)
                .memoryCacheKey("thumb_${mediaItem.id}")
                .diskCacheKey("media_${mediaItem.id}")
                .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                .placeholderMemoryCacheKey("thumb_${mediaItem.id}")
                .crossfade(false)
                .allowHardware(true)
                .build(),
            imageLoader        = loader,
            contentDescription = null,
            contentScale       = ContentScale.Fit,
            onSuccess          = { s ->
                val d = s.result.drawable
                if (d != null) intrinsicSize = IntSize(d.intrinsicWidth, d.intrinsicHeight)
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX       = scaleAnim.value
                    scaleY       = scaleAnim.value
                    translationX = panX.value
                    translationY = panY.value
                }
        )
    }
}

private enum class GestureMode { UNDECIDED, ZOOM_PAN, PASS_THROUGH }

@Composable
internal fun ExpressiveSheetCard(
    icon: ImageVector, label: String,
    containerColor: Color, contentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick  = onClick,
        color    = containerColor,
        shape    = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                color    = contentColor.copy(alpha = 0.12f),
                shape    = RoundedCornerShape(12.dp),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = contentColor, modifier = Modifier.size(22.dp))
                }
            }
            Text(label, style = MaterialTheme.typography.titleMedium, color = contentColor)
        }
    }
}

@Composable
private fun MediaInfoContent(item: MediaItem, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val dateStr = remember(item.dateAdded) {
        SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.getDefault())
            .format(Date(item.dateAdded * 1000L))
    }
    val sizeStr = remember(item.size) { Formatter.formatShortFileSize(context, item.size) }
    Column(modifier = modifier) {
        Text(
            stringResource(R.string.viewer_info_title),
            style      = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            modifier   = Modifier.padding(bottom = 20.dp)
        )
        InfoRow(stringResource(R.string.viewer_info_name), item.name)
        InfoDivider()
        InfoRow(stringResource(R.string.viewer_info_date), dateStr)
        InfoDivider()
        InfoRow(stringResource(R.string.viewer_info_size), sizeStr)
        if (item.width != null && item.height != null) {
            InfoDivider()
            InfoRow(stringResource(R.string.viewer_info_dimensions), "${item.width} × ${item.height} px")
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(label,
            style    = MaterialTheme.typography.bodyMedium,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f))
        Text(value,
            style    = MaterialTheme.typography.bodyMedium,
            color    = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.6f).padding(start = 8.dp))
    }
}

@Composable
private fun InfoDivider() =
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
