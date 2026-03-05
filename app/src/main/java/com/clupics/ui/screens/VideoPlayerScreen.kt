package com.clupics.ui.screens

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.clupics.R
import com.clupics.cast.CastManager
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    uri             : Uri,
    title           : String  = "",
    isActivePlayer  : Boolean = true,
    // showControls y onToggleControls los gestiona MediaViewerScreen desde afuera
    showControls    : Boolean = true,
    onToggleControls: () -> Unit = {}
) {
    val context = LocalContext.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = false
        }
    }

    LaunchedEffect(isActivePlayer) {
        exoPlayer.playWhenReady = isActivePlayer
    }

    DisposableEffect(Unit) { onDispose { exoPlayer.release() } }

    var isPlaying       by remember { mutableStateOf(false) }
    var isBuffering     by remember { mutableStateOf(true) }
    var videoEnded      by remember { mutableStateOf(false) }
    var duration        by remember { mutableLongStateOf(0L) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var isSeeking       by remember { mutableStateOf(false) }
    var seekPosition    by remember { mutableFloatStateOf(0f) }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) duration = exoPlayer.duration.coerceAtLeast(1L)
                if (state == Player.STATE_ENDED) { videoEnded = true }
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    LaunchedEffect(Unit) {
        if (CastManager.isConnected(context)) CastManager.castVideo(context, uri, title)
    }

    // Polling de posición — 16 ms para slider fluido
    LaunchedEffect(exoPlayer) {
        while (true) {
            if (!isSeeking) currentPosition = exoPlayer.currentPosition
            if (duration <= 1L) duration = exoPlayer.duration.coerceAtLeast(1L)
            delay(16)
        }
    }

    // Auto-oculta controles tras 3.5 s mientras reproduce
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(3500)
            onToggleControls()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) { detectTapGestures { onToggleControls() } }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    // Configurar todo ANTES de asignar el player para que el controlador
                    // nativo de ExoPlayer nunca aparezca ni por un frame
                    useController      = false
                    controllerAutoShow = false
                    hideController()
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    artworkDisplayMode = PlayerView.ARTWORK_DISPLAY_MODE_OFF
                    resizeMode         = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    player             = exoPlayer
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Indicador de buffering propio (ondas)
        if (isBuffering && isActivePlayer) {
            WavyBufferingIndicator(
                color    = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .align(Alignment.TopCenter)
            )
        }

        // ── Controles inferiores de reproducción ──────────────────────────────
        AnimatedVisibility(
            visible  = showControls,
            enter    = fadeIn(tween(120)),
            exit     = fadeOut(tween(80)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp, top = 8.dp)
            ) {
                // Tiempos
                Row(
                    modifier              = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text  = formatTime(if (isSeeking) (seekPosition * duration).toLong() else currentPosition),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        color = Color.White
                    )
                    Text(
                        text  = formatTime(duration),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }

                val progress = if (duration > 0 && !isSeeking)
                    currentPosition.toFloat() / duration.toFloat()
                else seekPosition

                Slider(
                    value         = progress.coerceIn(0f, 1f),
                    onValueChange = { value ->
                        isSeeking    = true
                        seekPosition = value
                        exoPlayer.seekTo((value * duration).toLong())
                    },
                    onValueChangeFinished = {
                        currentPosition = (seekPosition * duration).toLong()
                        isSeeking = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors   = SliderDefaults.colors(
                        thumbColor         = MaterialTheme.colorScheme.primary,
                        activeTrackColor   = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                    )
                )

                Spacer(Modifier.size(8.dp))

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    FilledTonalIconButton(
                        onClick  = {
                            val p = (exoPlayer.currentPosition - 10_000).coerceAtLeast(0)
                            exoPlayer.seekTo(p); currentPosition = p
                        },
                        modifier = Modifier.size(64.dp),
                        shape    = CircleShape,
                        colors   = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = Color.White.copy(alpha = 0.18f),
                            contentColor   = Color.White
                        )
                    ) {
                        Icon(Icons.Filled.Replay10, stringResource(R.string.player_rewind), modifier = Modifier.size(34.dp))
                    }

                    FilledIconButton(
                        onClick  = {
                            if (videoEnded) {
                                exoPlayer.seekTo(0); exoPlayer.play(); videoEnded = false
                            } else {
                                if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                            }
                        },
                        modifier = Modifier.size(80.dp),
                        shape    = CircleShape
                    ) {
                        Icon(
                            imageVector = when {
                                videoEnded -> Icons.Filled.Replay
                                isPlaying  -> Icons.Filled.Pause
                                else       -> Icons.Filled.PlayArrow
                            },
                            contentDescription = stringResource(when {
                                videoEnded -> R.string.player_replay
                                isPlaying  -> R.string.player_pause
                                else       -> R.string.player_play
                            }),
                            modifier = Modifier.size(44.dp)
                        )
                    }

                    FilledTonalIconButton(
                        onClick  = {
                            val p = (exoPlayer.currentPosition + 10_000).coerceAtMost(duration)
                            exoPlayer.seekTo(p); currentPosition = p
                        },
                        modifier = Modifier.size(64.dp),
                        shape    = CircleShape,
                        colors   = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = Color.White.copy(alpha = 0.18f),
                            contentColor   = Color.White
                        )
                    ) {
                        Icon(Icons.Filled.Forward10, stringResource(R.string.player_forward), modifier = Modifier.size(34.dp))
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes      = totalSeconds / 60
    val seconds      = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

@Composable
private fun WavyBufferingIndicator(
    color      : Color,
    modifier   : Modifier = Modifier,
    amplitude  : Dp = 2.dp,
    strokeWidth: Dp = 3.dp
) {
    val transition = rememberInfiniteTransition(label = "wavy")
    val phase by transition.animateFloat(
        initialValue  = 0f,
        targetValue   = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavyPhase"
    )
    Canvas(modifier = modifier) {
        val amplitudePx   = amplitude.toPx()
        val strokeWidthPx = strokeWidth.toPx()
        val midY          = size.height / 2f
        val wavelength    = size.width / 3f
        val path = Path()
        var first = true
        var x = 0f
        while (x <= size.width) {
            val y = midY + amplitudePx * sin((2f * PI * x / wavelength) - phase).toFloat()
            if (first) { path.moveTo(x, y); first = false } else path.lineTo(x, y)
            x += 2f
        }
        drawPath(path, color, style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round))
    }
}
