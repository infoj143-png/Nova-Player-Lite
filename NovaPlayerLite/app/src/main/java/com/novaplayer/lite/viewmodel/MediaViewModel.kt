package com.novaplayer.lite.viewmodel

import android.app.RecoverableSecurityException
import android.content.Context
import android.content.IntentSender
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novaplayer.lite.data.MediaScanner
import com.novaplayer.lite.data.models.AppStats
import com.novaplayer.lite.data.models.MediaItem
import com.novaplayer.lite.data.models.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

enum class SortOrder {
    NAME, DATE_ADDED, SIZE, DURATION
}

class MediaViewModel : ViewModel() {

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _recentMedia = MutableStateFlow<List<MediaItem>>(emptyList())
    val recentMedia: StateFlow<List<MediaItem>> = _recentMedia.asStateFlow()

    private val _videos = MutableStateFlow<List<MediaItem>>(emptyList())
    val videos: StateFlow<List<MediaItem>> = _videos.asStateFlow()

    private val _music = MutableStateFlow<List<MediaItem>>(emptyList())
    val audios: StateFlow<List<MediaItem>> = _music.asStateFlow()

    private val _videoSearchQuery = MutableStateFlow("")
    val videoSearchQuery = _videoSearchQuery.asStateFlow()

    private val _musicSearchQuery = MutableStateFlow("")
    val musicSearchQuery = _musicSearchQuery.asStateFlow()

    private val _videoSortOrder = MutableStateFlow(SortOrder.DATE_ADDED)
    val videoSortOrder = _videoSortOrder.asStateFlow()

    private val _musicSortOrder = MutableStateFlow(SortOrder.DATE_ADDED)
    val musicSortOrder = _musicSortOrder.asStateFlow()

    private val _isVideoGridView = MutableStateFlow(false)
    val isVideoGridView = _isVideoGridView.asStateFlow()

    private val _pendingDeleteRequest = MutableStateFlow<IntentSender?>(null)
    val pendingDeleteRequest = _pendingDeleteRequest.asStateFlow()

    val filteredVideos = combine(_videos, _videoSearchQuery, _videoSortOrder) { videos, query, sort ->
        videos.filter { it.title.contains(query, ignoreCase = true) }
            .let { sortMedia(it, sort) }
    }

    val filteredMusic = combine(_music, _musicSearchQuery, _musicSortOrder) { music, query, sort ->
        music.filter { it.title.contains(query, ignoreCase = true) }
            .let { sortMedia(it, sort) }
    }

    private val _favorites = MutableStateFlow<List<MediaItem>>(emptyList())
    val favorites: StateFlow<List<MediaItem>> = _favorites.asStateFlow()

    private val _stats = MutableStateFlow(AppStats(0, 0, "0 B"))
    val stats: StateFlow<AppStats> = _stats.asStateFlow()

    private var sharedPreferences: SharedPreferences? = null

    fun initPrefs(context: Context) {
        if (sharedPreferences == null) {
            sharedPreferences = context.getSharedPreferences("NovaPlayerPrefs", Context.MODE_PRIVATE)
            _isVideoGridView.value = sharedPreferences?.getBoolean("video_grid_view", false) ?: false
            loadFavorites()
            loadCachedMedia()
            loadRecent()
        }
    }

    fun scanMedia(context: Context) {
        initPrefs(context)
        viewModelScope.launch {
            _isScanning.value = true
            _errorMessage.value = null
            try {
                val scanner = MediaScanner(context)

                val scannedVideos = try {
                    withContext(Dispatchers.IO) { scanner.scanVideos() }
                } catch (e: Exception) {
                    e.printStackTrace()
                    emptyList()
                }

                val scannedMusic = try {
                    withContext(Dispatchers.IO) { scanner.scanAudio() }
                } catch (e: Exception) {
                    e.printStackTrace()
                    emptyList()
                }

                val favoritesIds = getFavoriteIds()

                val videosWithFavs = scannedVideos.map { it.copy(isFavorite = favoritesIds.contains(it.id.toString())) }
                val musicWithFavs = scannedMusic.map { it.copy(isFavorite = favoritesIds.contains(it.id.toString())) }

                _videos.value = videosWithFavs
                _music.value = musicWithFavs

                updateStats(videosWithFavs, musicWithFavs)
                updateFavoritesList()
                saveMediaToCache(_videos.value, _music.value)
                loadRecent() // Refresh recent list with full items
            } catch (e: Exception) {
                _errorMessage.value = "Failed to scan media: ${e.message}"
            } finally {
                _isScanning.value = false
            }
        }
    }

    private fun sortMedia(list: List<MediaItem>, sortOrder: SortOrder): List<MediaItem> {
        return when (sortOrder) {
            SortOrder.NAME -> list.sortedBy { it.title }
            SortOrder.DATE_ADDED -> list.sortedByDescending { it.dateAdded }
            SortOrder.SIZE -> list.sortedByDescending { it.size }
            SortOrder.DURATION -> list.sortedByDescending { it.duration }
        }
    }

    fun setVideoSearchQuery(query: String) { _videoSearchQuery.value = query }
    fun setMusicSearchQuery(query: String) { _musicSearchQuery.value = query }
    fun setVideoSortOrder(sortOrder: SortOrder) { _videoSortOrder.value = sortOrder }
    fun setMusicSortOrder(sortOrder: SortOrder) { _musicSortOrder.value = sortOrder }

    fun toggleVideoLayout() {
        _isVideoGridView.value = !_isVideoGridView.value
        sharedPreferences?.edit()?.putBoolean("video_grid_view", _isVideoGridView.value)?.apply()
    }

    fun getMediaByPath(path: String): MediaItem? {
        return (_videos.value + _music.value).find { it.path == path }
    }

    private fun updateStats(videos: List<MediaItem>, music: List<MediaItem>) {
        val totalSize = videos.sumOf { it.size } + music.sumOf { it.size }
        _stats.value = AppStats(
            totalVideos = videos.size,
            totalSongs = music.size,
            storageUsed = formatSize(totalSize)
        )
    }

    private fun formatSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format("%.2f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }

    fun toggleFavorite(item: MediaItem) {
        val currentIds = getFavoriteIds().toMutableSet()
        val itemId = item.id.toString()
        if (currentIds.contains(itemId)) {
            currentIds.remove(itemId)
        } else {
            currentIds.add(itemId)
        }
        saveFavoriteIds(currentIds)

        _videos.value = _videos.value.map { if (it.id == item.id) it.copy(isFavorite = !it.isFavorite) else it }
        _music.value = _music.value.map { if (it.id == item.id) it.copy(isFavorite = !it.isFavorite) else it }

        updateFavoritesList()
    }

    private fun updateFavoritesList() {
        _favorites.value = (_videos.value + _music.value).filter { it.isFavorite }
    }

    private fun getFavoriteIds(): Set<String> {
        return sharedPreferences?.getStringSet("favorites", emptySet()) ?: emptySet()
    }

    private fun saveFavoriteIds(ids: Set<String>) {
        sharedPreferences?.edit()?.putStringSet("favorites", ids)?.apply()
    }

    fun addToRecent(item: MediaItem) {
        val currentRecent = _recentMedia.value.toMutableList()
        currentRecent.removeAll { it.id == item.id }
        currentRecent.add(0, item)
        if (currentRecent.size > 20) {
            currentRecent.removeAt(currentRecent.size - 1)
        }
        _recentMedia.value = currentRecent
        saveRecent(currentRecent)
    }

    private fun saveRecent(recent: List<MediaItem>) {
        val jsonArray = JSONArray()
        recent.forEach { jsonArray.put(it.id) }
        sharedPreferences?.edit()?.putString("recent_ids", jsonArray.toString())?.apply()
    }

    private fun loadRecent() {
        val jsonString = sharedPreferences?.getString("recent_ids", null) ?: return
        val jsonArray = JSONArray(jsonString)
        val ids = mutableListOf<Long>()
        for (i in 0 until jsonArray.length()) {
            ids.add(jsonArray.getLong(i))
        }
        updateRecentListFromIds(ids)
    }

    private fun updateRecentListFromIds(ids: List<Long>) {
        val allMedia = _videos.value + _music.value
        if (allMedia.isEmpty()) return

        val recent = ids.mapNotNull { id -> allMedia.find { it.id == id } }
        _recentMedia.value = recent
    }

    private fun loadFavorites() {
        updateFavoritesList()
    }

    fun clearRecent() {
        _recentMedia.value = emptyList()
        sharedPreferences?.edit()?.remove("recent_ids")?.apply()
    }

    fun deleteMedia(context: Context, item: MediaItem) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val uri = Uri.parse(item.uri)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, listOf(uri))
                    _pendingDeleteRequest.value = pendingIntent.intentSender
                } else {
                    try {
                        context.contentResolver.delete(uri, null, null)
                        withContext(Dispatchers.Main) {
                            removeMediaFromList(item)
                        }
                    } catch (e: SecurityException) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val recoverableSecurityException = e as? RecoverableSecurityException
                                ?: throw e
                            _pendingDeleteRequest.value = recoverableSecurityException.userAction.actionIntent.intentSender
                        } else {
                            throw e
                        }
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete: ${e.message}"
            }
        }
    }

    fun removeMediaFromList(item: MediaItem) {
        if (item.type == MediaType.VIDEO) {
            _videos.value = _videos.value.filter { it.id != item.id }
        } else {
            _music.value = _music.value.filter { it.id != item.id }
        }
        _recentMedia.value = _recentMedia.value.filter { it.id != item.id }
        _favorites.value = _favorites.value.filter { it.id != item.id }
        updateStats(_videos.value, _music.value)
        saveMediaToCache(_videos.value, _music.value)
    }

    fun clearPendingDeleteRequest() {
        _pendingDeleteRequest.value = null
    }

    private fun saveMediaToCache(videos: List<MediaItem>, music: List<MediaItem>) {
        viewModelScope.launch(Dispatchers.IO) {
            val videosJson = JSONArray()
            videos.forEach { videosJson.put(it.toJson()) }

            val musicJson = JSONArray()
            music.forEach { musicJson.put(it.toJson()) }

            sharedPreferences?.edit()?.apply {
                putString("cached_videos", videosJson.toString())
                putString("cached_music", musicJson.toString())
                apply()
            }
        }
    }

    private fun loadCachedMedia() {
        val videosJsonStr = sharedPreferences?.getString("cached_videos", null)
        val musicJsonStr = sharedPreferences?.getString("cached_music", null)

        if (videosJsonStr != null) {
            val videos = mutableListOf<MediaItem>()
            val jsonArray = JSONArray(videosJsonStr)
            for (i in 0 until jsonArray.length()) {
                MediaItem.fromJson(jsonArray.getJSONObject(i))?.let { videos.add(it) }
            }
            _videos.value = videos
        }

        if (musicJsonStr != null) {
            val music = mutableListOf<MediaItem>()
            val jsonArray = JSONArray(musicJsonStr)
            for (i in 0 until jsonArray.length()) {
                MediaItem.fromJson(jsonArray.getJSONObject(i))?.let { music.add(it) }
            }
            _music.value = music
        }

        if (_videos.value.isNotEmpty() || _music.value.isNotEmpty()) {
            updateStats(_videos.value, _music.value)
            updateFavoritesList()
        }
    }
}

private fun MediaItem.toJson(): JSONObject {
    return JSONObject().apply {
        put("id", id)
        put("title", title)
        put("artist", artist)
        put("duration", duration)
        put("durationText", durationText)
        put("type", type.name)
        put("uri", uri)
        put("size", size)
        put("sizeText", sizeText)
        put("dateAdded", dateAdded)
        put("path", path)
        put("thumbnailUri", thumbnailUri)
        put("isFavorite", isFavorite)
    }
}

private fun MediaItem.Companion.fromJson(json: JSONObject): MediaItem? {
    return try {
        MediaItem(
            id = json.getLong("id"),
            title = json.getString("title"),
            artist = json.getString("artist"),
            duration = json.getLong("duration"),
            durationText = json.getString("durationText"),
            type = MediaType.valueOf(json.getString("type")),
            uri = json.getString("uri"),
            size = json.getLong("size"),
            sizeText = json.getString("sizeText"),
            dateAdded = json.getLong("dateAdded"),
            path = json.getString("path"),
            thumbnailUri = if (json.isNull("thumbnailUri")) {
                if (json.isNull("thumbnail")) null else json.getString("thumbnail")
            } else {
                json.getString("thumbnailUri")
            },
            isFavorite = json.optBoolean("isFavorite", false)
        )
    } catch (e: Exception) {
        null
    }
}
