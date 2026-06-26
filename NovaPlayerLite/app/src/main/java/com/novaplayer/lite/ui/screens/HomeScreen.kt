package com.novaplayer.lite.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import android.net.Uri
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.novaplayer.lite.R
import com.novaplayer.lite.data.models.MediaType
import com.novaplayer.lite.ui.navigation.Screen
import com.novaplayer.lite.ui.components.GlassCard
import com.novaplayer.lite.ui.components.NeonButton
import com.novaplayer.lite.ui.components.NeonBackground
import com.novaplayer.lite.ui.theme.NeonBlue
import com.novaplayer.lite.ui.theme.NeonPink
import com.novaplayer.lite.ui.theme.NeonPurple
import com.novaplayer.lite.viewmodel.MediaViewModel

@Composable
fun HomeScreen(viewModel: MediaViewModel, navController: NavController) {
    val stats by viewModel.stats.collectAsState()
    val recentMedia by viewModel.recentMedia.collectAsState()

    NeonBackground {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Column {
                    Text(
                        text = stringResource(R.string.welcome_back),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                    Text(
                        text = stringResource(R.string.user_name),
                        style = MaterialTheme.typography.headlineLarge
                    )
                }
            }

            item {
                Text(text = stringResource(R.string.app_stats), style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    GlassCard(modifier = Modifier.weight(1f)) {
                        Text(text = "${stats.totalVideos}", style = MaterialTheme.typography.headlineMedium, color = NeonBlue)
                        Text(text = "Videos", style = MaterialTheme.typography.bodyMedium)
                    }
                    GlassCard(modifier = Modifier.weight(1f)) {
                        Text(text = "${stats.totalSongs}", style = MaterialTheme.typography.headlineMedium, color = NeonPurple)
                        Text(text = "Songs", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(text = stats.storageUsed, style = MaterialTheme.typography.headlineSmall, color = NeonPink)
                    Text(text = "Storage Used", style = MaterialTheme.typography.bodyMedium)
                }
            }

            if (recentMedia.isNotEmpty()) {
                item {
                    Text(text = stringResource(R.string.recent_media), style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(recentMedia) { item ->
                            GlassCard(
                                modifier = Modifier.width(170.dp),
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
                                Box(
                                    modifier = Modifier
                                        .height(100.dp)
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.3f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (item.type == MediaType.VIDEO && item.thumbnail != null) {
                                        AsyncImage(
                                            model = item.thumbnail,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = if (item.type == MediaType.VIDEO) NeonBlue.copy(alpha = 0.7f) else NeonPurple.copy(alpha = 0.7f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1
                                )
                                Text(
                                    text = item.artist,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(text = stringResource(R.string.quick_actions), style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    NeonButton(
                        text = "Play All",
                        onClick = { },
                        color = NeonBlue,
                        modifier = Modifier.weight(1f)
                    )
                    NeonButton(
                        text = "Shuffle",
                        onClick = { },
                        color = NeonPink,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}
