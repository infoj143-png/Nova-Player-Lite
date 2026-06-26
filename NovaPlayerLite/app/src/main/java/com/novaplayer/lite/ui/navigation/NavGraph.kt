package com.novaplayer.lite.ui.navigation

import android.net.Uri
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.novaplayer.lite.ui.screens.*
import com.novaplayer.lite.viewmodel.MediaViewModel

@Composable
fun NavGraph(
    navController: NavHostController,
    viewModel: MediaViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() }
    ) {
        composable(Screen.Home.route) {
            HomeScreen(viewModel, navController)
        }
        composable(Screen.Videos.route) {
            VideosScreen(viewModel, navController)
        }
        composable(Screen.Music.route) {
            MusicScreen(viewModel, navController)
        }
        composable(Screen.Favorites.route) {
            FavoritesScreen(viewModel, navController)
        }
        composable(Screen.Settings.route) {
            SettingsScreen()
        }
        composable(
            route = Screen.VideoPlayer.route,
            arguments = listOf(navArgument("index") { type = NavType.IntType })
        ) { backStackEntry ->
            val index = backStackEntry.arguments?.getInt("index") ?: 0
            VideoPlayerScreen(viewModel, index, navController)
        }
        composable(
            route = Screen.AudioPlayer.route,
            arguments = listOf(navArgument("index") { type = NavType.IntType })
        ) { backStackEntry ->
            val index = backStackEntry.arguments?.getInt("index") ?: 0
            AudioPlayerScreen(viewModel, index, navController)
        }
    }
}
