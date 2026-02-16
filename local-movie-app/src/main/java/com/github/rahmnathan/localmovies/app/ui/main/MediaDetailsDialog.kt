package com.github.rahmnathan.localmovies.app.ui.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.github.rahmnathan.localmovies.app.media.data.Media

// Gradient colors for primary buttons
private val GradientStart = Color(0xFF667eea)
private val GradientEnd = Color(0xFF764ba2)

@Composable
fun MediaDetailsDialog(
    media: Media,
    isFavorite: Boolean = media.favorite,
    onDismiss: () -> Unit,
    onPlay: (resumePosition: Long) -> Unit,
    onFavoriteClick: () -> Unit = {}
) {
    val resumePosition = remember(media.mediaFileId) { media.getResumePosition() }
    val showResume = resumePosition != null && resumePosition >= 60000

    // Animate favorite button
    var animateFavorite by remember { mutableStateOf(false) }
    val favoriteScale by animateFloatAsState(
        targetValue = if (animateFavorite) 1.3f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        finishedListener = { animateFavorite = false },
        label = "favoriteScale"
    )

    val favoriteColor by animateColorAsState(
        targetValue = if (isFavorite) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(300),
        label = "favoriteColor"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = media.title,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = {
                        animateFavorite = true
                        onFavoriteClick()
                    },
                    modifier = Modifier.scale(favoriteScale)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = favoriteColor
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Poster with rounded corners
                val posterUrl = remember(media.mediaFileId) { media.signedUrls?.poster }
                if (posterUrl != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(2f / 3f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        AsyncImage(
                            model = posterUrl,
                            contentDescription = media.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                // Ratings and Year chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!media.releaseYear.isNullOrBlank()) {
                        AssistChip(
                            onClick = {},
                            label = { Text(media.releaseYear!!, fontWeight = FontWeight.Medium) }
                        )
                    }
                    if (!media.imdbRating.isNullOrBlank()) {
                        AssistChip(
                            onClick = {},
                            label = { Text("IMDb: ${media.imdbRating}", fontWeight = FontWeight.Medium) }
                        )
                    }
                    if (!media.metaRating.isNullOrBlank()) {
                        AssistChip(
                            onClick = {},
                            label = { Text("Meta: ${media.metaRating}", fontWeight = FontWeight.Medium) }
                        )
                    }
                }

                // Genre
                if (!media.genre.isNullOrBlank()) {
                    Text(
                        text = "Genre: ${media.genre}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Plot
                if (!media.plot.isNullOrBlank()) {
                    Text(
                        text = media.plot!!,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4
                    )
                }

                // Actors
                if (!media.actors.isNullOrBlank()) {
                    Text(
                        text = "Cast: ${media.actors}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Primary action with gradient
                if (media.streamable && showResume) {
                    GradientButton(
                        text = "Resume (${formatResumeTime(resumePosition!!)})",
                        onClick = { onPlay(resumePosition) }
                    )

                    FilledTonalButton(
                        onClick = { onPlay(0) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Play from Start", fontWeight = FontWeight.SemiBold)
                    }
                } else if (media.streamable) {
                    GradientButton(
                        text = "Play",
                        onClick = { onPlay(0) }
                    )
                } else {
                    GradientButton(
                        text = "Open",
                        onClick = { onPlay(0) }
                    )
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        },
        dismissButton = null
    )
}

@Composable
private fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    Modifier.background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(GradientStart, GradientEnd)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = Color.White
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = text,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Formats milliseconds to a human-readable resume time string.
 * E.g., 3661000 -> "1h 1m"
 */
internal fun formatResumeTime(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60

    return if (hours > 0) {
        String.format("%dh %dm", hours, minutes)
    } else {
        String.format("%dm", minutes)
    }
}
