package com.github.rahmnathan.localmovies.app.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.NotInterested
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.github.rahmnathan.localmovies.app.media.data.Recommendation

private val RecommendationCardShape = RoundedCornerShape(16.dp)
private val ThumbnailShape = RoundedCornerShape(12.dp)

@Composable
internal fun RecommendationsList(
    recommendations: List<Recommendation>,
    isLoading: Boolean,
    onMediaClick: (Media) -> Unit,
    onPlayMedia: (Media) -> Unit,
    onMoreLikeThis: (Media) -> Unit,
    onNotInterested: (Media) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "header") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Text(
                    text = "Recommended for You",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Based on your watch history",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(
            items = recommendations,
            key = { it.media.mediaFileId }
        ) { recommendation ->
            RecommendationCard(
                recommendation = recommendation,
                onClick = { onMediaClick(recommendation.media) },
                onPlay = { onPlayMedia(recommendation.media) },
                onMoreLikeThis = { onMoreLikeThis(recommendation.media) },
                onNotInterested = { onNotInterested(recommendation.media) }
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
private fun RecommendationCard(
    recommendation: Recommendation,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    onMoreLikeThis: () -> Unit,
    onNotInterested: () -> Unit
) {
    val media = recommendation.media
    val (confidenceLabel, confidenceColor) = recommendationConfidence(recommendation.rank)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RecommendationCardShape,
                ambientColor = Color.Black.copy(alpha = 0.3f),
                spotColor = Color.Black.copy(alpha = 0.3f)
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = RecommendationCardShape
            ),
        shape = RecommendationCardShape,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .width(85.dp)
                        .aspectRatio(2f / 3f)
                        .shadow(6.dp, ThumbnailShape)
                        .clip(ThumbnailShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (media.signedUrls?.poster != null) {
                        AsyncImage(
                            model = media.signedUrls.poster,
                            contentDescription = media.title,
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

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = media.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        media.releaseYear?.let { year ->
                            Text(
                                text = year,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        media.imdbRating?.let { rating ->
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = "★ $rating",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = confidenceColor
                    ) {
                        Text(
                            text = confidenceLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    media.genre?.let { genre ->
                        Text(
                            text = genre,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    recommendation.reason?.let { reason ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = reason,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (media.streamable) {
                    FilledIconButton(
                        onClick = onPlay,
                        modifier = Modifier.size(48.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onMoreLikeThis,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.TravelExplore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("More like this")
                }

                TextButton(
                    onClick = onNotInterested,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.NotInterested,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Not interested")
                }
            }
        }
    }
}

private fun recommendationConfidence(rank: Int): Pair<String, Color> {
    return when {
        rank in 1..3 -> "High match" to Color(0xFFBDDDBA)
        rank in 4..8 -> "Medium match" to Color(0xFFE9D9A0)
        else -> "Low match" to Color(0xFFE8C4C4)
    }
}
