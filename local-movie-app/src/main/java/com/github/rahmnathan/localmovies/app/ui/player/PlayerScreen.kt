package com.github.rahmnathan.localmovies.app.ui.player

import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel = hiltViewModel(),
    onNavigateToNextEpisode: (url: String, updatePositionUrl: String, mediaId: String) -> Unit = { _, _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (uiState.videoUrl.isNotBlank()) {
            // Log the URL for debugging
            LaunchedEffect(uiState.videoUrl) {
                android.util.Log.d("PlayerScreen", "Video URL: ${uiState.videoUrl}")
                android.util.Log.d("PlayerScreen", "Update URL: ${uiState.updatePositionUrl}")
            }

            var videoView by remember { mutableStateOf<VideoView?>(null) }

            AndroidView(
                factory = { context ->
                    VideoView(context).apply {
                        // Setup media controller
                        val mediaController = MediaController(context)
                        mediaController.setAnchorView(this)
                        setMediaController(mediaController)

                        // Setup listeners
                        setOnPreparedListener { mediaPlayer ->
                            android.util.Log.d("PlayerScreen", "Video prepared, starting playback")
                            // Seek to saved position if available
                            if (uiState.currentPosition > 0) {
                                mediaPlayer.seekTo(uiState.currentPosition.toInt())
                            }

                            // Start playback
                            start()
                            viewModel.onPlaybackStarted()
                        }

                        setOnCompletionListener {
                            android.util.Log.d("PlayerScreen", "Video completed")
                            viewModel.onPlaybackPaused()
                        }

                        setOnErrorListener { _, what, extra ->
                            android.util.Log.e("PlayerScreen", "Video error: what=$what, extra=$extra")
                            viewModel.onPlaybackPaused()
                            false // Return false to show default error dialog
                        }

                        videoView = this
                    }
                },
                update = { view ->
                    // Set video URI when URL changes
                    if (uiState.videoUrl.isNotBlank()) {
                        try {
                            val uri = uiState.videoUrl.toUri()
                            android.util.Log.d("PlayerScreen", "Setting video URI: $uri")
                            view.setVideoURI(uri)
                        } catch (e: Exception) {
                            android.util.Log.e("PlayerScreen", "Error setting video URI", e)
                        }
                    }

                    // Update position periodically
                    if (view.isPlaying) {
                        viewModel.onPositionChanged(view.currentPosition.toLong())
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Error state
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Invalid video URL",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        // Show error if any
        if (uiState.error != null) {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(all = 16.dp),
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("Dismiss")
                    }
                }
            ) {
                Text(uiState.error!!)
            }
        }
    }

    // Cleanup when screen is disposed
    DisposableEffect(Unit) {
        onDispose {
            viewModel.onPlaybackPaused()
        }
    }
}
