package com.novaplayer.lite.ui.screens

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.novaplayer.lite.R
import com.novaplayer.lite.ui.components.GlassCard
import com.novaplayer.lite.ui.components.NeonButton
import com.novaplayer.lite.ui.theme.NeonBlue
import com.novaplayer.lite.ui.theme.NeonPink
import com.novaplayer.lite.ui.theme.NeonPurple
import com.novaplayer.lite.viewmodel.MediaViewModel

@Composable
fun HomeScreen(viewModel: MediaViewModel) {
    val stats by viewModel.stats.collectAsState()
    val recentMedia by viewModel.recentMedia.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
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
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
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
        }

        item {
            Text(text = stringResource(R.string.recent_media), style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(recentMedia) { item ->
                    GlassCard(modifier = Modifier.width(160.dp)) {
                        Box(modifier = Modifier.height(90.dp)) {
                            // Placeholder for thumbnail
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = NeonBlue, modifier = Modifier.fillMaxSize())
                        }
                        Text(text = item.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                        Text(text = item.artist, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                    }
                }
            }
        }

        item {
            Text(text = stringResource(R.string.quick_actions), style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
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

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}
