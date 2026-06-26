package com.novaplayer.lite.ui.screens

import android.app.Activity
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.novaplayer.lite.data.models.MediaItem
import com.novaplayer.lite.ui.components.GlassCard
import com.novaplayer.lite.ui.components.GlassSearchBar
import com.novaplayer.lite.ui.components.NeonBackground
import com.novaplayer.lite.ui.navigation.Screen
import com.novaplayer.lite.ui.theme.NeonBlue
import com.novaplayer.lite.viewmodel.MediaViewModel
import com.novaplayer.lite.viewmodel.SortOrder

@Composable
fun VideosScreen(viewModel: MediaViewModel, navController: NavController) {
    val videos by viewModel.filteredVideos.collectAsState(initial = emptyList())
    val searchQuery by viewModel.videoSearchQuery.collectAsState()
    val isLoading by viewModel.isScanning.collectAsState()
    val isGridView by viewModel.isVideoGridView.collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var videoToDelete by remember { mutableStateOf<MediaItem?>(null) }
    val pendingDeleteRequest by viewModel.pendingDeleteRequest.collectAsState()

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            videoToDelete?.let { viewModel.removeMediaFromList(it) }
            scope.launch {
                snackbarHostState.showSnackbar("Video deleted successfully")
            }
        }
        videoToDelete = null
        viewModel.clearPendingDeleteRequest()
    }

    LaunchedEffect(pendingDeleteRequest) {
        pendingDeleteRequest?.let {
            deleteLauncher.launch(IntentSenderRequest.Builder(it).build())
        }
    }

    if (videoToDelete != null && pendingDeleteRequest == null) {
        AlertDialog(
            onDismissRequest = { videoToDelete = null },
            title = { Text("Delete Video") },
            text = { Text("Are you sure you want to delete this video?") },
            confirmButton = {
                TextButton(onClick = {
                    videoToDelete?.let { viewModel.deleteMedia(context, it) }
                }) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { videoToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    NeonBackground {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp)
            ) {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Videos", style = MaterialTheme.typography.headlineLarge)
                Row {
                    IconButton(onClick = { viewModel.toggleVideoLayout() }) {
                        Icon(
                            if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                            contentDescription = "Toggle Layout",
                            tint = NeonBlue
                        )
                    }
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort", tint = NeonBlue)
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            SortOrder.values().forEach { order ->
                                DropdownMenuItem(
                                    text = { Text(order.name.replace("_", " ")) },
                                    onClick = {
                                        viewModel.setVideoSortOrder(order)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            GlassSearchBar(
                value = searchQuery,
                onValueChange = { viewModel.setVideoSearchQuery(it) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonBlue, strokeWidth = 3.dp)
                }
            } else if (videos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (searchQuery.isEmpty()) "No videos found.\nScan your device to find media." else "No videos matching search.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                if (isGridView) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(bottom = 100.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(videos) { video ->
                            VideoGridItem(
                                video = video,
                                onClick = {
                                    val index = viewModel.videos.value.indexOfFirst { it.path == video.path }
                                    if (index != -1) {
                                        navController.navigate(Screen.VideoPlayer.route.replace("{index}", index.toString()))
                                    }
                                },
                                onFavoriteToggle = { viewModel.toggleFavorite(video) },
                                onDelete = { videoToDelete = video }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(bottom = 100.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(videos) { video ->
                            VideoListItem(
                                video = video,
                                onClick = {
                                    val index = viewModel.videos.value.indexOfFirst { it.path == video.path }
                                    if (index != -1) {
                                        navController.navigate(Screen.VideoPlayer.route.replace("{index}", index.toString()))
                                    }
                                },
                                onFavoriteToggle = { viewModel.toggleFavorite(video) },
                                onDelete = { videoToDelete = video }
                            )
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
fun VideoListItem(
    video: MediaItem,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onDelete: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
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
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(video.thumbnailUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = painterResource(id = android.R.drawable.ic_menu_slideshow),
                    fallback = painterResource(id = android.R.drawable.ic_menu_slideshow)
                )
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = NeonBlue.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${video.durationText} • ${video.sizeText}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
            IconButton(onClick = onFavoriteToggle) {
                Icon(
                    if (video.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = if (video.isFavorite) Color.Red else Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun VideoGridItem(
    video: MediaItem,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onDelete: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        padding = 0.dp
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(video.thumbnailUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = painterResource(id = android.R.drawable.ic_menu_slideshow),
                    fallback = painterResource(id = android.R.drawable.ic_menu_slideshow)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .background(Color.Black.copy(alpha = 0.6f), MaterialTheme.shapes.extraSmall)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = video.durationText,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp
                    )
                }
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                ) {
                    IconButton(
                        onClick = onFavoriteToggle,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            if (video.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = if (video.isFavorite) Color.Red else Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = video.sizeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        }
    }
}
