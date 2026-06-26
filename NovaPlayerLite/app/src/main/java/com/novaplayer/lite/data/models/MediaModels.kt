package com.novaplayer.lite.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

enum class MediaType {
    VIDEO, AUDIO
}

@Parcelize
data class MediaItem(
    val id: Long,
    val title: String,
    val artist: String,
    val duration: Long,
    val durationText: String,
    val type: MediaType,
    val uri: String,
    val size: Long,
    val sizeText: String,
    val dateAdded: Long,
    val path: String,
    val thumbnailUri: String? = null,
    val isFavorite: Boolean = false
) : Parcelable {
    companion object
}

data class AppStats(
    val totalVideos: Int,
    val totalSongs: Int,
    val storageUsed: String
)
