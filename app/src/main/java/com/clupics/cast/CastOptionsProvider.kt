package com.clupics.cast

import android.content.Context
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider
import com.google.android.gms.cast.framework.media.CastMediaOptions
import com.google.android.gms.cast.framework.media.NotificationOptions

/**
 * Configura el SDK de Cast.
 * Usa el receptor predeterminado de Google (Default Media Receiver),
 * compatible con Chromecast, Google TV, Android TV y cualquier
 * dispositivo con Google Cast. Para Roku se usa DIAL/mDNS vía MediaRouter.
 */
class CastOptionsProvider : OptionsProvider {

    override fun getCastOptions(context: Context): CastOptions {
        val notificationOptions = NotificationOptions.Builder()
            .setTargetActivityClassName(Class.forName("com.clupics.MainActivity").name)
            .build()

        val mediaOptions = CastMediaOptions.Builder()
            .setNotificationOptions(notificationOptions)
            .build()

        return CastOptions.Builder()
            // Default Media Receiver — funciona sin registro de app en Cast Console
            .setReceiverApplicationId("CC1AD845")
            .setCastMediaOptions(mediaOptions)
            .build()
    }

    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider>? = null
}
