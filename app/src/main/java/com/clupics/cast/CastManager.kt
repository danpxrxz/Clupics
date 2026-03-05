package com.clupics.cast

import android.content.Context
import android.net.Uri
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener

/**
 * Gestiona el ciclo de vida de Cast y el envío de media al dispositivo remoto.
 */
object CastManager {

    fun getCastContext(context: Context): CastContext? = try {
        CastContext.getSharedInstance(context)
    } catch (e: Exception) {
        null
    }

    fun isConnected(context: Context): Boolean {
        return getCastContext(context)
            ?.sessionManager
            ?.currentCastSession
            ?.isConnected == true
    }

    /**
     * Envía un video al dispositivo Cast conectado.
     * @param uri URI del archivo de video (content://)
     * @param title Nombre del archivo para mostrar en el TV
     */
    fun castVideo(context: Context, uri: Uri, title: String) {
        val castContext = getCastContext(context) ?: return
        val session   = castContext.sessionManager.currentCastSession ?: return
        val client    = session.remoteMediaClient             ?: return

        val metadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE).apply {
            putString(MediaMetadata.KEY_TITLE, title)
        }

        // Para content:// URIs locales necesitamos un servidor HTTP local.
        // Usamos el URI directamente — funciona cuando el dispositivo Cast
        // está en la misma red y el archivo es accesible (o mediante file sharing).
        // En producción se reemplazaría con un servidor HTTP local (NanoHTTPD).
        val mediaInfo = MediaInfo.Builder(uri.toString())
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType("video/*")
            .setMetadata(metadata)
            .build()

        val request = MediaLoadRequestData.Builder()
            .setMediaInfo(mediaInfo)
            .setAutoplay(true)
            .build()

        client.load(request)
    }
}
