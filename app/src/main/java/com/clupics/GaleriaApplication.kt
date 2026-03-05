package com.clupics

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.clupics.utils.GaleriaImageLoader

/**
 * Implementa ImageLoaderFactory — forma CORRECTA de registrar un ImageLoader
 * global en Coil 2.x. Reemplaza Coil.setImageLoader() que está deprecado
 * y en algunos dispositivos no se aplica antes del primer request.
 */
class GaleriaApplication : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader =
        GaleriaImageLoader.get(this)
}
