package com.github.rahmnathan.localmovies.app.ui.cast

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay

@Composable
fun CastMiniController(
    viewModel: CastControllerViewModel = hiltViewModel(),
    onClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Update playback position periodically
    LaunchedEffect(uiState.isPlaying) {
        while (uiState.isPlaying) {
            viewModel.updatePosition()
            delay(1000)
        }
    }

    // Don't show if not connected or no media
    if (!uiState.isConnected || uiState.title.isBlank()) {
        android.util.Log.d("CastMiniController", "Not showing - isConnected: ${uiState.isConnected}, title: '${uiState.title}'")
        return
    }

    android.util.Log.d("CastMiniController", "Showing mini controller - title: ${uiState.title}, isPlaying: ${uiState.isPlaying}")

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 3.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Progress bar
            LinearProgressIndicator(
                progress = {
                    if (uiState.duration > 0) {
                        uiState.currentPosition.toFloat() / uiState.duration.toFloat()
                    } else {
                        0f
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = uiState.title,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Casting to ${getCastDeviceName()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = {
                        if (uiState.isPlaying) {
                            viewModel.pause()
                        } else {
                            viewModel.play()
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (uiState.isPlaying) "Pause" else "Play"
                    )
                }
            }
        }
    }
}

@Composable
private fun getCastDeviceName(): String {
    // You can inject CastContext here to get the actual device name
    // For now, return a generic name
    return "TV"
}
