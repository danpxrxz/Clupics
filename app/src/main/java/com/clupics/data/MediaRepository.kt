package com.clupics.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

class MediaRepository(private val context: Context) {

    suspend fun getImages(): List<MediaItem> = withContext(Dispatchers.IO) {
        val images = mutableListOf<MediaItem>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        else
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.BUCKET_ID,
        )

        context.contentResolver.query(
            collection, projection, null, null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idCol     = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol   = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateCol   = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val sizeCol   = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val mimeCol   = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
            val widthCol  = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val albumCol  = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val bucketCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)

            while (cursor.moveToNext()) {
                val id  = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(collection, id)
                images.add(MediaItem(
                    id        = id,
                    uri       = uri,
                    name      = cursor.getString(nameCol) ?: "Sin nombre",
                    dateAdded = cursor.getLong(dateCol),
                    size      = cursor.getLong(sizeCol),
                    mediaType = MediaType.IMAGE,
                    mimeType  = cursor.getString(mimeCol) ?: "image/*",
                    width     = cursor.getInt(widthCol),
                    height    = cursor.getInt(heightCol),
                    albumName = cursor.getString(albumCol),
                    bucketId  = cursor.getLong(bucketCol)
                ))
            }
        }
        images
    }

    suspend fun getVideos(): List<MediaItem> = withContext(Dispatchers.IO) {
        val videos = mutableListOf<MediaItem>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        else
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.BUCKET_ID,
        )

        context.contentResolver.query(
            collection, projection, null, null,
            "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idCol       = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol     = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val dateCol     = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val sizeCol     = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val mimeCol     = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            val widthCol    = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val heightCol   = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val albumCol    = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            val bucketCol   = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID)

            while (cursor.moveToNext()) {
                val id  = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(collection, id)
                videos.add(MediaItem(
                    id        = id,
                    uri       = uri,
                    name      = cursor.getString(nameCol) ?: "Sin nombre",
                    dateAdded = cursor.getLong(dateCol),
                    size      = cursor.getLong(sizeCol),
                    mediaType = MediaType.VIDEO,
                    mimeType  = cursor.getString(mimeCol) ?: "video/*",
                    width     = cursor.getInt(widthCol),
                    height    = cursor.getInt(heightCol),
                    duration  = cursor.getLong(durationCol),
                    albumName = cursor.getString(albumCol),
                    bucketId  = cursor.getLong(bucketCol)
                ))
            }
        }
        videos
    }

    suspend fun getAlbums(): List<Album> = withContext(Dispatchers.IO) {
        val albumMap = mutableMapOf<Long, MutableList<MediaItem>>()
        (getImages() + getVideos()).forEach { item ->
            albumMap.getOrPut(item.bucketId ?: -1L) { mutableListOf() }.add(item)
        }
        albumMap.entries.mapNotNull { (bucketId, items) ->
            val first = items.firstOrNull() ?: return@mapNotNull null
            Album(
                id        = bucketId,
                name      = first.albumName ?: "Desconocido",
                coverUri  = first.uri,
                itemCount = items.size,
                mediaType = if (items.any { it.mediaType == MediaType.IMAGE }) MediaType.IMAGE else MediaType.VIDEO
            )
        }.sortedByDescending { it.itemCount }
    }

    /** Images and videos queried IN PARALLEL — faster startup. */
    suspend fun getAllMedia(): List<MediaItem> = coroutineScope {
        val imagesDeferred = async(Dispatchers.IO) { getImages() }
        val videosDeferred = async(Dispatchers.IO) { getVideos() }
        (imagesDeferred.await() + videosDeferred.await()).sortedByDescending { it.dateAdded }
    }

    /**
     * Mueve archivos a otra carpeta.
     * Estrategia: intentar RELATIVE_PATH update primero (solo funciona en archivos propios);
     * si falla, copiar el contenido a un nuevo registro MediaStore y eliminar el original.
     */
    suspend fun moveMediaToAlbum(
        uris: List<Uri>,
        targetAlbumName: String,
        targetRelativePath: String
    ): List<Uri> = withContext(Dispatchers.IO) {
        val failed = mutableListOf<Uri>()
        val path = if (targetRelativePath.endsWith("/")) targetRelativePath else "$targetRelativePath/"

        for (uri in uris) {
            try {
                val moved = tryMoveViaUpdate(uri, path) || tryMoveViaCopy(uri, path)
                if (!moved) failed.add(uri)
            } catch (e: Exception) {
                failed.add(uri)
            }
        }
        failed
    }

    /** Intenta mover actualizando RELATIVE_PATH — funciona para archivos propios (Android 10+). */
    private fun tryMoveViaUpdate(uri: Uri, relativePath: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        return try {
            val cv = ContentValues().apply {
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            }
            context.contentResolver.update(uri, cv, null, null) > 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Copia el archivo a un nuevo registro en MediaStore con la ruta destino,
     * luego elimina el original. Funciona para archivos de otras apps (Android 10+).
     */
    private fun tryMoveViaCopy(uri: Uri, relativePath: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        return try {
            // Obtener metadatos del archivo original
            val projection = arrayOf(
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.MIME_TYPE,
                MediaStore.MediaColumns.SIZE
            )
            val cursor = context.contentResolver.query(uri, projection, null, null, null)
                ?: return false
            val (name, mimeType) = cursor.use { c ->
                if (!c.moveToFirst()) return false
                val n = c.getString(c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)) ?: return false
                val m = c.getString(c.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)) ?: "image/jpeg"
                n to m
            }

            // Determinar la colección destino según el tipo MIME
            val collection = when {
                mimeType.startsWith("video/") ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                        MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                else ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            // Insertar nuevo registro con IS_PENDING = 1
            val newValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val destUri = context.contentResolver.insert(collection, newValues) ?: return false

            // Copiar bytes
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    context.contentResolver.openOutputStream(destUri)?.use { output ->
                        input.copyTo(output)
                    }
                }
                // Marcar como listo
                val done = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
                context.contentResolver.update(destUri, done, null, null)
            } catch (e: Exception) {
                // Si la copia falla, eliminar el registro vacío
                context.contentResolver.delete(destUri, null, null)
                return false
            }

            // Eliminar el original — puede requerir permiso en Android 11+, intentar de todas formas
            try {
                context.contentResolver.delete(uri, null, null)
            } catch (_: Exception) {
                // Si no se puede borrar el original, la copia existe pero el original queda
                // No se considera un fallo de la operación
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Returns distinct album folders available on the device. */
    suspend fun getAlbumFolders(): List<AlbumFolder> = withContext(Dispatchers.IO) {
        val folders = mutableMapOf<String, AlbumFolder>()

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        else
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val projection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(
                MediaStore.Images.Media.BUCKET_ID,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Images.Media.RELATIVE_PATH
            )
        } else {
            arrayOf(
                MediaStore.Images.Media.BUCKET_ID,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Images.Media.DATA
            )
        }

        context.contentResolver.query(collection, projection, null, null, null)?.use { c ->
            val bucketCol = c.getColumnIndex(MediaStore.Images.Media.BUCKET_ID)
            val nameCol   = c.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val pathCol   = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                c.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
            else
                c.getColumnIndex(MediaStore.Images.Media.DATA)

            while (c.moveToNext()) {
                val name    = c.getString(nameCol) ?: continue
                val rawPath = c.getString(pathCol) ?: continue
                val relPath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) rawPath
                else rawPath.substringBeforeLast("/")
                    .substringAfter("/storage/emulated/0/")
                if (!folders.containsKey(name)) {
                    folders[name] = AlbumFolder(
                        bucketId     = c.getLong(bucketCol),
                        name         = name,
                        relativePath = relPath
                    )
                }
            }
        }
        folders.values.sortedBy { it.name }
    }
}

data class AlbumFolder(
    val bucketId     : Long,
    val name         : String,
    val relativePath : String
)