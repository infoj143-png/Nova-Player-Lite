package com.novaplayer.lite.ui.screens

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import com.novaplayer.lite.data.models.MediaItem
import com.novaplayer.lite.ui.components.formatTime
import androidx.lifecycle.viewModelScope
import com.novaplayer.lite.ui.theme.NeonBlue
import com.novaplayer.lite.viewmodel.MediaViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun VideoPlayerScreen(
    viewModel: MediaViewModel,
    mediaPath: String,
    navController: NavController
) {
    val context = LocalContext.current
    val mediaItem = remember(mediaPath) { viewModel.getMediaByPath(mediaPath) }

    var isPlaying by remember { mutableStateOf(false) }
    var playbackState by remember { mutableIntStateOf(Player.STATE_IDLE) }
    var duration by remember { mutableLongStateOf(0L) }
    var isFirstFrameRendered by remember { mutableStateOf(false) }
    var isFullscreen by rememberSaveable { mutableStateOf(false) }
    var showControls by rememberSaveable { mutableStateOf(true) }
    var isLocked by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // Gesture States
    var gestureType by remember { mutableStateOf<String?>(null) } // "volume" or "brightness"
    var gestureValue by remember { mutableFloatStateOf(0f) }
    var showGestureOverlay by remember { mutableStateOf(false) }
    var gestureJob by remember { mutableStateOf<Job?>(null) }

    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val activity = context as? Activity

    val exoPlayer = remember {
        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                2500, // Min buffer
                5000, // Max buffer
                1000, // Buffer for playback
                1500  // Buffer for playback after rebuffer
            )
            .build()

        ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
            .setLoadControl(loadControl)
            .build().apply {
            mediaItem?.let {
                setMediaItem(Media3Item.fromUri(it.uri))
                prepare()
                playWhenReady = true
                viewModel.addToRecent(it)
            }
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }

                override fun onPlaybackStateChanged(state: Int) {
                    playbackState = state
                    if (state == Player.STATE_READY) {
                        duration = this@apply.duration
                    }
                }

                override fun onRenderedFirstFrame() {
                    isFirstFrameRendered = true
                }

                override fun onPlayerError(e: PlaybackException) {
                    error = "Failed to play video: ${e.message}"
                }
            })
        }
    }

    LaunchedEffect(showControls) {
        if (showControls && isPlaying) {
            delay(5000)
            showControls = false
        }
    }

    val view = LocalView.current
    DisposableEffect(isFullscreen) {
        val activity = context as? Activity
        val window = activity?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, view)
            if (isFullscreen) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {}
    }

    DisposableEffect(Unit) {
        onDispose {
            val activity = context as? Activity
            val window = activity?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            exoPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(isLocked) {
                if (!isLocked) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            gestureJob?.cancel()
                            val isRightSide = offset.x > (size.width / 2)
                            gestureType = if (isRightSide) "volume" else "brightness"

                            if (gestureType == "volume") {
                                val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                gestureValue = (currentVol.toFloat() / maxVol * 100f)
                            } else {
                                val lp = activity?.window?.attributes
                                var currentBrightness = lp?.screenBrightness ?: -1f
                                if (currentBrightness < 0) currentBrightness = 0.5f
                                gestureValue = currentBrightness * 100f
                            }
                            showGestureOverlay = true
                        },
                        onDragEnd = {
                            gestureJob?.cancel()
                            gestureJob = viewModel.viewModelScope.launch {
                                delay(1000)
                                showGestureOverlay = false
                                gestureType = null
                            }
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            val sensitivity = 0.2f
                            val delta = -(dragAmount * sensitivity)
                            gestureValue = (gestureValue + delta).coerceIn(0f, 100f)

                            if (gestureType == "volume") {
                                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                val newVol = (gestureValue / 100f * maxVol).roundToInt()
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                            } else {
                                activity?.window?.let { window ->
                                    val lp = window.attributes
                                    lp.screenBrightness = gestureValue / 100f
                                    window.attributes = lp
                                }
                            }
                        }
                    )
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { if (!isLocked) showControls = !showControls }
    ) {
        AndroidView(
            factory = {
                PlayerView(context).apply {
                    player = exoPlayer
                    useController = false
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        AnimatedVisibility(
            visible = showControls && !isLocked,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 }
        ) {
            VideoControls(
                exoPlayer = exoPlayer,
                isPlaying = isPlaying,
                duration = duration,
                isFullscreen = isFullscreen,
                isLocked = isLocked,
                onPlayPause = { if (isPlaying) exoPlayer.pause() else exoPlayer.play() },
                onSeek = { exoPlayer.seekTo(it) },
                onForward = { exoPlayer.seekTo(exoPlayer.currentPosition + 10000) },
                onBackward = { exoPlayer.seekTo(exoPlayer.currentPosition - 10000) },
                onFullscreenToggle = { isFullscreen = !isFullscreen },
                onLockToggle = { isLocked = !isLocked },
                onBack = { navController.popBackStack() },
                title = mediaItem?.title ?: "Video"
            )
        }

        // Floating Unlock Button when locked
        AnimatedVisibility(
            visible = isLocked,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                IconButton(
                    onClick = { isLocked = false },
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(
                        Icons.Default.LockOpen,
                        contentDescription = "Unlock",
                        tint = NeonBlue,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        // Gesture Overlay
        AnimatedVisibility(
            visible = showGestureOverlay,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            GestureOverlay(
                icon = if (gestureType == "volume") Icons.Default.VolumeUp else Icons.Default.BrightnessMedium,
                value = gestureValue.roundToInt(),
                label = if (gestureType == "volume") "Volume" else "Brightness"
            )
        }

        if (playbackState == Player.STATE_BUFFERING || (playbackState == Player.STATE_READY && !isFirstFrameRendered)) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = NeonBlue,
                strokeWidth = 3.dp
            )
        }

        error?.let {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Error, contentDescription = null, tint = Color.Red, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = it, color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { navController.popBackStack() },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonBlue)
                    ) {
                        Text("Go Back", color = Color.Black)
                    }
                }
            }
        }
    }
}


@Composable
fun VideoControls(
    exoPlayer: ExoPlayer,
    isPlaying: Boolean,
    duration: Long,
    isFullscreen: Boolean,
    isLocked: Boolean,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onForward: () -> Unit,
    onBackward: () -> Unit,
    onFullscreenToggle: () -> Unit,
    onLockToggle: () -> Unit,
    onBack: () -> Unit,
    title: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent, Color.Black.copy(alpha = 0.6f))
                )
            )
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.3f))
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                maxLines = 1,
                modifier = Modifier.weight(1f).padding(start = 16.dp)
            )
            IconButton(
                onClick = onLockToggle,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.3f))
            ) {
                Icon(
                    if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = "Toggle Lock",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onFullscreenToggle,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.3f))
            ) {
                Icon(
                    if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    contentDescription = "Toggle Fullscreen",
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackward) {
                Icon(Icons.Default.Replay10, contentDescription = "-10s", tint = Color.White, modifier = Modifier.size(42.dp))
            }
            Spacer(modifier = Modifier.width(48.dp))

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(NeonBlue.copy(alpha = 0.9f))
                    .clickable { onPlayPause() },
                contentAlignment = Alignment.Center
            ) {
                Crossfade(targetState = isPlaying, label = "PlayPause") { playing ->
                    Icon(
                        if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.Black,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(48.dp))
            IconButton(onClick = onForward) {
                Icon(Icons.Default.Forward10, contentDescription = "+10s", tint = Color.White, modifier = Modifier.size(42.dp))
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        PlaybackProgress(exoPlayer = exoPlayer, duration = duration, onSeek = onSeek)
    }
}

@Composable
fun GestureOverlay(icon: ImageVector, value: Int, label: String) {
    Surface(
        color = Color.Black.copy(alpha = 0.7f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.size(120.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(icon, contentDescription = null, tint = NeonBlue, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "$value%", color = Color.White, fontWeight = FontWeight.Bold)
            Text(text = label, color = Color.Gray, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun PlaybackProgress(
    exoPlayer: ExoPlayer,
    duration: Long,
    onSeek: (Long) -> Unit
) {
    var currentPosition by remember { mutableLongStateOf(exoPlayer.currentPosition) }

    LaunchedEffect(exoPlayer) {
        while (true) {
            currentPosition = exoPlayer.currentPosition
            delay(1000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.4f))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Slider(
            value = currentPosition.toFloat(),
            onValueChange = {
                currentPosition = it.toLong()
                onSeek(it.toLong())
            },
            valueRange = 0f..duration.coerceAtLeast(0L).toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = NeonBlue,
                activeTrackColor = NeonBlue,
                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = formatTime(currentPosition), color = Color.White, style = MaterialTheme.typography.bodySmall)
            Text(text = formatTime(duration), color = Color.White, style = MaterialTheme.typography.bodySmall)
        }
    }
}
