package com.github.rahmnathan.localmovies.app.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.ui.PlayerView

@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel = hiltViewModel(),
    onNavigateToNextEpisode: (url: String, updatePositionUrl: String, mediaId: String, resumePosition: Long) -> Unit = { _, _, _, _ -> },
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Set up callback for next episode navigation
    LaunchedEffect(Unit) {
        viewModel.setNextEpisodeCallback { nextEpisode ->
            onNavigateToNextEpisode(
                nextEpisode.streamUrl,
                nextEpisode.updatePositionUrl,
                nextEpisode.mediaId,
                0L
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ExoPlayer PlayerView
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    // Attach ExoPlayer to PlayerView
                    player = viewModel.player

                    // Configure PlayerView
                    useController = true // Show playback controls
                    controllerShowTimeoutMs = 3000 // Hide controls after 3 seconds
                    controllerHideOnTouch = false
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Back button in top-left corner (overlays the player)
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        // Show loading indicator
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White
            )
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

        // Next episode countdown overlay
        if (uiState.showNextEpisodeCountdown && uiState.nextEpisode != null) {
            NextEpisodeCountdown(
                nextEpisode = uiState.nextEpisode!!,
                countdownSeconds = uiState.countdownSeconds,
                onPlayNow = { viewModel.playNextEpisode() },
                onCancel = { viewModel.cancelNextEpisodeCountdown() },
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}

@Composable
private fun NextEpisodeCountdown(
    nextEpisode: NextEpisodeInfo,
    countdownSeconds: Int,
    onPlayNow: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .padding(24.dp)
            .widthIn(max = 320.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.Black.copy(alpha = 0.85f),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with cancel button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Up Next",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            // Episode info
            val episodeText = if (!nextEpisode.episodeNumber.isNullOrBlank()) {
                "E${nextEpisode.episodeNumber} - ${nextEpisode.title}"
            } else {
                nextEpisode.title
            }
            Text(
                text = episodeText,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2
            )

            // Countdown
            Text(
                text = "Playing in $countdownSeconds...",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )

            // Play now button
            Button(
                onClick = onPlayNow,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                )
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Play Now")
            }
        }
    }
}
