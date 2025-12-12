package com.github.rahmnathan.localmovies.app.data.remote.dto

import com.github.rahmnathan.localmovies.app.media.data.Media
import com.google.gson.annotations.SerializedName

/**
 * DTO that matches the actual API response structure:
 * - File fields are at the root level (fileName, path, mediaFileId, etc.)
 * - Metadata is nested in a 'media' object
 */
data class MediaResponseDto(
    @SerializedName("fileName")
    val fileName: String,

    @SerializedName("path")
    val path: String,

    @SerializedName("mediaFileId")
    val mediaFileId: String,

    @SerializedName("streamable")
    val streamable: Boolean?,

    @SerializedName("created")
    val created: String?,  // API returns as string timestamp

    @SerializedName("media")
    val media: MediaDto?
) {
    fun toMedia(): Media {
        // Determine file type from path extension
        val fileType = when {
            path.endsWith(".mp4", ignoreCase = true) -> "VIDEO"
            path.endsWith(".mkv", ignoreCase = true) -> "VIDEO"
            path.endsWith(".avi", ignoreCase = true) -> "VIDEO"
            else -> "DIRECTORY"
        }

        return Media(
            // Fields from nested media object
            title = media?.title ?: fileName,  // Fallback to fileName if no title
            imdbRating = media?.imdbRating,
            metaRating = media?.metaRating,
            image = media?.image,
            releaseYear = media?.releaseYear,
            created = created?.toLongOrNull(),  // Convert string to Long
            genre = media?.genre,
            actors = media?.actors,
            plot = media?.plot,
            number = media?.number,

            // Fields from root level
            filename = fileName,
            path = path,
            type = fileType,
            mediaFileId = mediaFileId,
            streamable = streamable ?: false
        )
    }
}

data class MediaDto(
    @SerializedName("title")
    val title: String?,

    @SerializedName("imdbRating")
    val imdbRating: String?,

    @SerializedName("metaRating")
    val metaRating: String?,

    @SerializedName("image")
    val image: String?,

    @SerializedName("releaseYear")
    val releaseYear: String?,

    @SerializedName("genre")
    val genre: String?,

    @SerializedName("actors")
    val actors: String?,

    @SerializedName("plot")
    val plot: String?,

    @SerializedName("number")
    val number: String?
)
