package com.clupics.cast

import android.graphics.PorterDuff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.DrawableCompat
import androidx.mediarouter.app.MediaRouteButton
import androidx.mediarouter.media.MediaControlIntent
import androidx.mediarouter.media.MediaRouteSelector
import com.google.android.gms.cast.framework.CastButtonFactory

/**
 * Botón de Cast nativo — usa MediaRouteButton de AndroidX.
 * [tint] fuerza el color del icono (usar Color.White en el visor oscuro).
 */
@Composable
fun CastButton(
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified
) {
    val selector = remember {
        MediaRouteSelector.Builder()
            .addControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
            .build()
    }
    val tintArgb = if (tint != Color.Unspecified) tint.toArgb() else null

    AndroidView(
        modifier = modifier,
        factory = { context ->
            MediaRouteButton(context).apply {
                routeSelector = selector
                try { CastButtonFactory.setUpMediaRouteButton(context, this) } catch (_: Exception) {}
                if (tintArgb != null) applyTint(this, tintArgb)
            }
        },
        update = { button ->
            if (tintArgb != null) applyTint(button, tintArgb)
        }
    )
}

private fun applyTint(button: MediaRouteButton, colorArgb: Int) {
    try {
        val field = MediaRouteButton::class.java.getDeclaredField("mRemoteIndicator")
        field.isAccessible = true
        val drawable = field.get(button) as? android.graphics.drawable.Drawable ?: return
        val wrapped = DrawableCompat.wrap(drawable.mutate())
        DrawableCompat.setTint(wrapped, colorArgb)
        DrawableCompat.setTintMode(wrapped, PorterDuff.Mode.SRC_IN)
        button.invalidate()
    } catch (_: Exception) {}
}
