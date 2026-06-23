package com.novaplayer.lite.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

enum class MediaType {
    VIDEO, AUDIO
}

@Parcelize
data class MediaItem(
    val id: String,
    val title: String,
    val artist: String,
    val duration: String,
    val type: MediaType,
    val thumbnail: String? = null,
    val isFavorite: Boolean = false
) : Parcelable

data class AppStats(
    val totalVideos: Int,
    val totalSongs: Int,
    val storageUsed: String
)
