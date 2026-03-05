package com.clupics.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.clupics.data.Album
import com.clupics.data.AlbumFolder
import com.clupics.data.MediaItem
import com.clupics.data.MediaRepository
import com.clupics.data.MediaType
import com.clupics.ui.screens.SortOrder
import com.clupics.ui.screens.applySort

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GaleriaUiState(
    val allMedia: List<MediaItem> = emptyList(),
    val filteredMedia: List<MediaItem> = emptyList(),
    val albums: List<Album> = emptyList(),
    val albumFolders: List<AlbumFolder> = emptyList(),
    val isLoading: Boolean = false,
    val hasPermission: Boolean = false,
    val error: String? = null
)

class GaleriaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MediaRepository(application)

    private val _uiState = MutableStateFlow(GaleriaUiState())
    val uiState: StateFlow<GaleriaUiState> = _uiState.asStateFlow()

    // Home sort order — shared so viewer pager matches grid order
    val homeSortOrder = MutableStateFlow(SortOrder.DATE_DESC)

    fun setHomeSortOrder(order: SortOrder) { homeSortOrder.value = order }

    fun getSortedHomeMedia(): List<com.clupics.data.MediaItem> =
        _uiState.value.filteredMedia.applySort(homeSortOrder.value)

    // Guard: only load once automatically — subsequent loads must be explicit (pull-to-refresh)
    private var initialLoadDone = false

    fun onPermissionGranted() {
        _uiState.value = _uiState.value.copy(hasPermission = true)
        // Only trigger automatic load once per ViewModel lifetime
        if (!initialLoadDone) {
            initialLoadDone = true
            loadMedia()
        }
    }

    fun loadMedia() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val allMedia     = repository.getAllMedia()
                val albums       = repository.getAlbums()
                val albumFolders = repository.getAlbumFolders()
                _uiState.value = _uiState.value.copy(
                    allMedia      = allMedia,
                    filteredMedia = allMedia,
                    albums        = albums,
                    albumFolders  = albumFolders,
                    isLoading     = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error al cargar multimedia: ${e.message}"
                )
            }
        }
    }

    fun applyFilters(showImages: Boolean, showVideos: Boolean) {
        val filtered = _uiState.value.allMedia.filter { item ->
            when (item.mediaType) {
                MediaType.IMAGE -> showImages
                MediaType.VIDEO -> showVideos
                MediaType.AUDIO -> true
            }
        }
        _uiState.value = _uiState.value.copy(filteredMedia = filtered)
    }

    fun getMediaForAlbum(albumId: Long): List<MediaItem> =
        _uiState.value.allMedia.filter { it.bucketId == albumId }

    fun getMediaItem(mediaId: Long): MediaItem? =
        _uiState.value.allMedia.firstOrNull { it.id == mediaId }

    fun moveMediaToAlbum(
        uris: List<Uri>,
        targetFolder: AlbumFolder,
        onComplete: (failed: Int) -> Unit
    ) {
        viewModelScope.launch {
            val failed = repository.moveMediaToAlbum(uris, targetFolder.name, targetFolder.relativePath)
            loadMedia()
            onComplete(failed.size)
        }
    }

    fun getMediaUrisForAlbum(albumId: Long): List<Uri> =
        _uiState.value.allMedia.filter { it.bucketId == albumId }.map { it.uri }

    // ── Shared-element open rect ──────────────────────────────────────────────
    // Stores the screen-space bounding box of the tapped thumbnail so the viewer
    // can animate expanding from that exact position (and collapsing back to it).
    data class ThumbRect(val x: Float, val y: Float, val w: Float, val h: Float)
    val openThumbRect = MutableStateFlow<ThumbRect?>(null)
    fun setOpenThumbRect(rect: ThumbRect?) { openThumbRect.value = rect }

    // ── Grid scroll position persistence ─────────────────────────────────────
    // Saved so that returning from the viewer restores exact scroll position.
    data class GridScrollPos(val index: Int = 0, val offset: Int = 0)
    val homeGridScroll = MutableStateFlow(GridScrollPos())
    fun saveHomeGridScroll(index: Int, offset: Int) {
        homeGridScroll.value = GridScrollPos(index, offset)
    }
}
