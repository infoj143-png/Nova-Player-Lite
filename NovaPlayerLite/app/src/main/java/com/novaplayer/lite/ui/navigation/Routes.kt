package com.novaplayer.lite.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Videos : Screen("videos")
    object Music : Screen("music")
    object Favorites : Screen("favorites")
    object Settings : Screen("settings")
    object VideoPlayer : Screen("video_player/{index}")
    object AudioPlayer : Screen("audio_player/{index}")
}
