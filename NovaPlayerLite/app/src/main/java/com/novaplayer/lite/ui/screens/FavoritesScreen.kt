package com.novaplayer.lite.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import android.net.Uri
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.novaplayer.lite.data.models.MediaType
import com.novaplayer.lite.ui.components.GlassCard
import com.novaplayer.lite.ui.components.NeonBackground
import com.novaplayer.lite.ui.navigation.Screen
import com.novaplayer.lite.ui.theme.NeonPink
import com.novaplayer.lite.viewmodel.MediaViewModel

@Composable
fun FavoritesScreen(viewModel: MediaViewModel, navController: NavController) {
    val favorites by viewModel.favorites.collectAsState()

    NeonBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "Favorites", style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(24.dp))

            if (favorites.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Color.Gray.copy(alpha = 0.3f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No favorites yet.\nHeart some media to see them here!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(favorites) { item ->
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                if (item.type == MediaType.VIDEO) {
                                    val index = viewModel.videos.value.indexOfFirst { it.path == item.path }
                                    if (index != -1) {
                                        navController.navigate(Screen.VideoPlayer.route.replace("{index}", index.toString()))
                                    }
                                } else {
                                    val index = viewModel.audios.value.indexOfFirst { it.path == item.path }
                                    if (index != -1) {
                                        navController.navigate(Screen.AudioPlayer.route.replace("{index}", index.toString()))
                                    }
                                }
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp, 50.dp)
                                        .background(Color.Black.copy(alpha = 0.3f), MaterialTheme.shapes.small),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (item.type == MediaType.VIDEO && item.thumbnailUri != null) {
                                        AsyncImage(
                                            model = item.thumbnailUri,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    Icon(
                                        Icons.Default.Favorite,
                                        contentDescription = null,
                                        tint = NeonPink.copy(alpha = 0.7f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${item.artist} • ${item.type.name.lowercase().replaceFirstChar { it.uppercase() }}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Gray
                                    )
                                }
                                IconButton(onClick = { viewModel.toggleFavorite(item) }) {
                                    Icon(
                                        Icons.Default.Favorite,
                                        contentDescription = null,
                                        tint = Color.Red,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(100.dp)) }
                }
            }
        }
    }
}
