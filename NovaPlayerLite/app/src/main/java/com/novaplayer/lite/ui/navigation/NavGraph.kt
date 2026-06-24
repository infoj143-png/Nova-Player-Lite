package com.novaplayer.lite.ui.navigation

import android.net.Uri
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
        startDestination = Screen.Home.route
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
            arguments = listOf(navArgument("mediaPath") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedPath = backStackEntry.arguments?.getString("mediaPath") ?: ""
            val mediaPath = Uri.decode(encodedPath)
            VideoPlayerScreen(viewModel, mediaPath, navController)
        }
        composable(
            route = Screen.AudioPlayer.route,
            arguments = listOf(navArgument("mediaPath") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedPath = backStackEntry.arguments?.getString("mediaPath") ?: ""
            val mediaPath = Uri.decode(encodedPath)
            AudioPlayerScreen(viewModel, mediaPath, navController)
        }
    }
}
