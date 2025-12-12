package com.github.rahmnathan.localmovies.app.ui.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.github.rahmnathan.localmovies.app.media.data.Media

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaDetailScreen(
    media: Media,
    onDismiss: () -> Unit,
    onPlayClick: () -> Unit,
    viewModel: MediaDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Set the media in the ViewModel
    LaunchedEffect(media) {
        viewModel.setMedia(media)
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.9f),
        properties = DialogProperties(), content = {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = MaterialTheme.shapes.large,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp)
                ) {
                    // Poster Image
                    if (!media.image.isNullOrBlank()) {
                        AsyncImage(
                            model = media.image,
                            contentDescription = media.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Title
                    Text(
                        text = media.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Rating chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (!media.releaseYear.isNullOrBlank()) {
                            AssistChip(
                                onClick = { },
                                label = { Text(media.releaseYear) }
                            )
                        }
                        if (!media.imdbRating.isNullOrBlank() && media.imdbRating != "0.0") {
                            AssistChip(
                                onClick = { },
                                label = { Text("IMDB: ${media.imdbRating}") }
                            )
                        }
                        if (!media.metaRating.isNullOrBlank() && media.metaRating != "0") {
                            AssistChip(
                                onClick = { },
                                label = { Text("Meta: ${media.metaRating}") }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Genre
                    if (!media.genre.isNullOrBlank()) {
                        Text(
                            text = "Genre",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = media.genre,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Plot
                    if (!media.plot.isNullOrBlank()) {
                        Text(
                            text = "Plot",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = media.plot,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Actors
                    if (!media.actors.isNullOrBlank()) {
                        Text(
                            text = "Cast",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = media.actors,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Close")
                        }
                        if (media.streamable) {
                            Button(onClick = onPlayClick) {
                                Text("Play")
                            }
                        }
                    }
                }
            }
        })
}
