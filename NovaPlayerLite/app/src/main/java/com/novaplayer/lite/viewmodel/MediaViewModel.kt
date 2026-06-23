package com.novaplayer.lite.viewmodel

import androidx.lifecycle.ViewModel
import com.novaplayer.lite.data.models.AppStats
import com.novaplayer.lite.data.models.MediaItem
import com.novaplayer.lite.data.models.MediaType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class MediaViewModel : ViewModel() {

    private val _recentMedia = MutableStateFlow<List<MediaItem>>(emptyList())
    val recentMedia: StateFlow<List<MediaItem>> = _recentMedia.asStateFlow()

    private val _videos = MutableStateFlow<List<MediaItem>>(emptyList())
    val videos: StateFlow<List<MediaItem>> = _videos.asStateFlow()

    private val _music = MutableStateFlow<List<MediaItem>>(emptyList())
    val music: StateFlow<List<MediaItem>> = _music.asStateFlow()

    private val _favorites = MutableStateFlow<List<MediaItem>>(emptyList())
    val favorites: StateFlow<List<MediaItem>> = _favorites.asStateFlow()

    private val _stats = MutableStateFlow(AppStats(0, 0, "0 GB"))
    val stats: StateFlow<AppStats> = _stats.asStateFlow()

    init {
        loadDummyData()
    }

    private fun loadDummyData() {
        val dummyVideos = listOf(
            MediaItem(UUID.randomUUID().toString(), "Cyberpunk 2077 Trailer", "CD Projekt Red", "2:30", MediaType.VIDEO),
            MediaItem(UUID.randomUUID().toString(), "Neon City Walk", "Tokyo Visuals", "5:15", MediaType.VIDEO),
            MediaItem(UUID.randomUUID().toString(), "Space Journey", "NASA", "10:00", MediaType.VIDEO),
            MediaItem(UUID.randomUUID().toString(), "Abstract Motion", "Design Co", "1:45", MediaType.VIDEO, isFavorite = true)
        )

        val dummyMusic = listOf(
            MediaItem(UUID.randomUUID().toString(), "Midnight City", "M83", "4:03", MediaType.AUDIO, isFavorite = true),
            MediaItem(UUID.randomUUID().toString(), "Blinding Lights", "The Weeknd", "3:20", MediaType.AUDIO),
            MediaItem(UUID.randomUUID().toString(), "Starboy", "The Weeknd", "3:50", MediaType.AUDIO),
            MediaItem(UUID.randomUUID().toString(), "Nightcall", "Kavinsky", "4:18", MediaType.AUDIO, isFavorite = true)
        )

        _videos.value = dummyVideos
        _music.value = dummyMusic
        _recentMedia.value = (dummyVideos + dummyMusic).shuffled().take(5)
        _favorites.value = (dummyVideos + dummyMusic).filter { it.isFavorite }
        _stats.value = AppStats(dummyVideos.size, dummyMusic.size, "12.5 GB")
    }

    fun toggleFavorite() {
        // Logic to toggle favorite
    }

    fun clearRecent() {
        _recentMedia.value = emptyList()
    }
}
