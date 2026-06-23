package com.novaplayer.lite.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Videos : Screen("videos")
    object Music : Screen("music")
    object Favorites : Screen("favorites")
    object Settings : Screen("settings")
}
