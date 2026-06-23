package com.novaplayer.lite.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.novaplayer.lite.ui.screens.*
import com.novaplayer.lite.viewmodel.MediaViewModel

@Composable
fun NavGraph(
    navController: NavHostController,
    viewModel: MediaViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(viewModel, navController)
        }
        composable(Screen.Videos.route) {
            VideosScreen(viewModel)
        }
        composable(Screen.Music.route) {
            MusicScreen(viewModel)
        }
        composable(Screen.Favorites.route) {
            FavoritesScreen(viewModel)
        }
        composable(Screen.Settings.route) {
            SettingsScreen()
        }
    }
}
