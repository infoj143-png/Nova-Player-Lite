package com.novaplayer.lite

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.novaplayer.lite.ui.navigation.NavGraph
import com.novaplayer.lite.ui.navigation.Screen
import com.novaplayer.lite.ui.theme.NovaPlayerLiteTheme
import com.novaplayer.lite.viewmodel.MediaViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NovaPlayerLiteTheme {
                MainLayout()
            }
        }
    }
}

@Composable
fun MainLayout() {
    val navController = rememberNavController()
    val viewModel: MediaViewModel = viewModel()

    val items = listOf(
        Screen.Home to (Icons.Default.Home to "Home"),
        Screen.Videos to (Icons.Default.VideoLibrary to "Videos"),
        Screen.Music to (Icons.Default.MusicNote to "Music"),
        Screen.Favorites to (Icons.Default.Favorite to "Favorites"),
        Screen.Settings to (Icons.Default.Settings to "Settings")
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.Transparent,
                contentColor = Color.White
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { (screen, iconTitle) ->
                    val (icon, title) = iconTitle
                    NavigationBarItem(
                        icon = { Icon(icon, contentDescription = title) },
                        label = { Text(title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = Color.Gray,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            NavGraph(navController = navController, viewModel = viewModel)
        }
    }
}
