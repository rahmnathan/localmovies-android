package com.github.rahmnathan.localmovies.app.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.github.rahmnathan.localmovies.app.media.data.Media

private val HistoryCardShape = RoundedCornerShape(16.dp)
private val ThumbnailShape = RoundedCornerShape(12.dp)

/**
 * Represents a group of history items - either a series with its most recent episode,
 * or a standalone movie.
 */
internal data class HistoryGroup(
    val id: String,
    val title: String,
    val posterUrl: String?,
    val isSeries: Boolean,
    val mostRecentEpisode: Media?,
    val movie: Media?
) {
    val displayMedia: Media get() = mostRecentEpisode ?: movie!!
}

/**
 * Groups media items by series and returns a list ready for display.
 * Episodes are grouped under their series, movies remain standalone.
 */
@Composable
internal fun rememberGroupedHistory(mediaList: List<Media>): List<HistoryGroup> {
    return remember(mediaList) {
        val groups = mutableMapOf<String, HistoryGroup>()

        for (media in mediaList) {
            val seriesId = media.getSeriesId()

            if (seriesId != null) {
                val existing = groups[seriesId]
                if (existing == null) {
                    val seriesTitle = media.getSeriesTitle() ?: media.title
                    val seriesPoster = media.parent?.getSeries()?.let {
                        media.signedUrls?.poster
                    } ?: media.signedUrls?.poster

                    groups[seriesId] = HistoryGroup(
                        id = seriesId,
                        title = seriesTitle,
                        posterUrl = seriesPoster,
                        isSeries = true,
                        mostRecentEpisode = media,
                        movie = null
                    )
                }
            } else {
                groups[media.mediaFileId] = HistoryGroup(
                    id = media.mediaFileId,
                    title = media.title,
                    posterUrl = media.signedUrls?.poster,
                    isSeries = false,
                    mostRecentEpisode = null,
                    movie = media
                )
            }
        }

        groups.values.toList()
    }
}

@Composable
internal fun HistoryList(
    mediaList: List<Media>,
    isLoading: Boolean,
    onPlayMedia: (Media, Long) -> Unit,
    onShowDetails: (Media) -> Unit
) {
    val groupedHistory = rememberGroupedHistory(mediaList)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = groupedHistory,
            key = { it.id }
        ) { group ->
            HistoryCard(
                group = group,
                onContinue = {
                    val media = group.displayMedia
                    val resumePosition = media.getResumePosition() ?: 0L
                    onPlayMedia(media, resumePosition)
                },
                onShowDetails = {
                    onShowDetails(group.displayMedia)
                }
            )
        }

        if (isLoading) {
            item(key = "loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(
    group: HistoryGroup,
    onContinue: () -> Unit,
    onShowDetails: () -> Unit
) {
    val media = group.displayMedia
    val resumePosition = remember(media.mediaFileId) { media.getResumePosition() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = HistoryCardShape,
                ambientColor = Color.Black.copy(alpha = 0.3f),
                spotColor = Color.Black.copy(alpha = 0.3f)
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = HistoryCardShape
            ),
        shape = HistoryCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onContinue)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Poster thumbnail with shadow
            Box(
                modifier = Modifier
                    .width(85.dp)
                    .aspectRatio(2f / 3f)
                    .shadow(6.dp, ThumbnailShape)
                    .clip(ThumbnailShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (group.posterUrl != null) {
                    AsyncImage(
                        model = group.posterUrl,
                        contentDescription = group.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        MaterialTheme.colorScheme.surface
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Title (series name or movie title)
                Text(
                    text = group.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Episode info for series
                if (group.isSeries && group.mostRecentEpisode != null) {
                    val episode = group.mostRecentEpisode
                    val seasonNum = episode.getSeasonNumber()
                    val episodeNum = episode.number

                    val episodeText = buildString {
                        if (seasonNum != null) append("S$seasonNum ")
                        if (!episodeNum.isNullOrBlank()) append("E$episodeNum")
                        if (isNotEmpty()) append(" · ")
                        append(episode.title)
                    }

                    Text(
                        text = episodeText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Resume position with accent color
                if (resumePosition != null && resumePosition > 0) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = "Resume at ${formatDuration(resumePosition)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Play button with gradient background
            FilledIconButton(
                onClick = onContinue,
                modifier = Modifier.size(48.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Continue",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            // Info button
            IconButton(
                onClick = onShowDetails
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Details",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Formats milliseconds to a human-readable duration string.
 * E.g., 3661000 -> "1h 1m"
 */
internal fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60

    return if (hours > 0) {
        "${hours}h ${minutes}m"
    } else {
        "${minutes}m"
    }
}
