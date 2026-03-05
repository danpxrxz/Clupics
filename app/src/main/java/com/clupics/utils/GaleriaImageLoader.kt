package com.clupics.utils

import android.content.Context
import android.net.Uri
import android.util.DisplayMetrics
import android.view.WindowManager
import coil.ImageLoader
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision

object GaleriaImageLoader {

    @Volatile private var instance: ImageLoader? = null

    fun get(context: Context): ImageLoader =
        instance ?: synchronized(this) {
            instance ?: build(context.applicationContext).also { instance = it }
        }

    /**
     * Calcula el tamaño óptimo de miniatura en px para la grilla.
     * Sin margen extra — el tamaño exacto maximiza aciertos de caché sin
     * desperdiciar memoria con píxeles que la grilla nunca muestra.
     * Redondea al múltiplo de 32 para reutilización de bitmaps.
     */
    fun thumbSizePx(context: Context, columns: Int = 3): Int {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val dm = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(dm)
        // Tamaño exacto por celda (sin margen extra) — menos memoria, más reutilización
        val rawPx = (dm.widthPixels.toFloat() / columns).toInt()
        return ((rawPx + 16) / 32) * 32
    }

    /**
     * Request óptima para miniatura de grilla.
     * - allowHardware(false): evita copias GPU→CPU que causan drops en Compose scroll.
     * - crossfade(false): sin fade = sin recomposición extra por frame de animación.
     * - Precision.INEXACT: reutiliza bitmaps de tamaño cercano en caché.
     * - placeholderMemoryCacheKey: muestra el bitmap cacheado inmediatamente (sin parpadeo).
     */
    fun requestFor(
        context : Context,
        mediaId : Long,
        uri     : Uri,
        sizePx  : Int
    ): ImageRequest {
        val key = "thumb_$mediaId"
        return ImageRequest.Builder(context)
            .data(uri)
            .size(sizePx, sizePx)
            .precision(Precision.INEXACT)
            .memoryCacheKey(key)
            .diskCacheKey(key)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.DISABLED)
            // false = bitmaps en RAM normal, Compose los dibuja sin copias extra → scroll fluido
            .allowHardware(false)
            .crossfade(false)
            .placeholderMemoryCacheKey(key)
            .build()
    }

    private fun build(context: Context): ImageLoader =
        ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder(context)
                    // 25 % de RAM: suficiente para la grilla visible sin presionar al GC
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("thumb_cache"))
                    // 64 MB es más que suficiente; 256 MB llenaba el almacenamiento
                    .maxSizeBytes(64L * 1024 * 1024)
                    .build()
            }
            .components { add(VideoFrameDecoder.Factory()) }
            .respectCacheHeaders(false)
            .crossfade(false)
            .networkCachePolicy(CachePolicy.DISABLED)
            // RGB_565 = mitad de memoria por bitmap (suficiente para miniaturas)
            .allowRgb565(true)
            // false global: bitmaps en RAM normal para scroll fluido en Compose
            .allowHardware(false)
            .build()
}
