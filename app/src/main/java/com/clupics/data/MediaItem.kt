package com.clupics.data

import android.net.Uri

enum class MediaType { IMAGE, VIDEO, AUDIO }

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val dateAdded: Long,
    val size: Long,
    val mediaType: MediaType,
    val mimeType: String,
    val duration: Long? = null,    // ms, for video/audio
    val width: Int? = null,
    val height: Int? = null,
    val albumName: String? = null,
    val bucketId: Long? = null
)

data class Album(
    val id: Long,
    val name: String,
    val coverUri: Uri?,
    val itemCount: Int,
    val mediaType: MediaType
)
