package com.novaplayer.lite.viewmodel

import android.content.Context
import android.content.SharedPreferences
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
    private val _music = MutableStateFlow<List<MediaItem>>(emptyList())

    private val _videoSearchQuery = MutableStateFlow("")
    val videoSearchQuery = _videoSearchQuery.asStateFlow()

    private val _musicSearchQuery = MutableStateFlow("")
    val musicSearchQuery = _musicSearchQuery.asStateFlow()

    private val _videoSortOrder = MutableStateFlow(SortOrder.DATE_ADDED)
    val videoSortOrder = _videoSortOrder.asStateFlow()

    private val _musicSortOrder = MutableStateFlow(SortOrder.DATE_ADDED)
    val musicSortOrder = _musicSortOrder.asStateFlow()

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
            loadFavorites()
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
                val scannedVideos = withContext(Dispatchers.IO) { scanner.scanVideos() }
                val scannedMusic = withContext(Dispatchers.IO) { scanner.scanAudio() }

                val favoritesIds = getFavoriteIds()

                val videosWithFavs = scannedVideos.map { it.copy(isFavorite = favoritesIds.contains(it.id.toString())) }
                val musicWithFavs = scannedMusic.map { it.copy(isFavorite = favoritesIds.contains(it.id.toString())) }

                _videos.value = videosWithFavs
                _music.value = musicWithFavs

                updateStats(videosWithFavs, musicWithFavs)
                updateFavoritesList()
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
}
