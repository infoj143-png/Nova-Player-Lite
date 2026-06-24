package com.novaplayer.lite.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import android.net.Uri
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.novaplayer.lite.ui.components.GlassCard
import com.novaplayer.lite.ui.components.GlassSearchBar
import com.novaplayer.lite.ui.navigation.Screen
import com.novaplayer.lite.ui.theme.NeonPurple
import com.novaplayer.lite.viewmodel.MediaViewModel
import com.novaplayer.lite.viewmodel.SortOrder

@Composable
fun MusicScreen(viewModel: MediaViewModel, navController: NavController) {
    val music by viewModel.filteredMusic.collectAsState(initial = emptyList())
    val searchQuery by viewModel.musicSearchQuery.collectAsState()
    val isLoading by viewModel.isScanning.collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Music", style = MaterialTheme.typography.headlineLarge)
            IconButton(onClick = { showSortMenu = true }) {
                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    SortOrder.values().forEach { order ->
                        DropdownMenuItem(
                            text = { Text(order.name.replace("_", " ")) },
                            onClick = {
                                viewModel.setMusicSortOrder(order)
                                showSortMenu = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        GlassSearchBar(
            value = searchQuery,
            onValueChange = { viewModel.setMusicSearchQuery(it) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NeonPurple)
            }
        } else if (music.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (searchQuery.isEmpty()) "No music found on device." else "No music matching search.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(music) { song ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val encodedPath = Uri.encode(song.path)
                            navController.navigate(Screen.AudioPlayer.route.replace("{mediaPath}", encodedPath))
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = NeonPurple,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${song.artist} • ${song.durationText}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                            }
                            IconButton(onClick = { viewModel.toggleFavorite(song) }) {
                                Icon(
                                    if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    tint = if (song.isFavorite) Color.Red else Color.Gray
                                )
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}
