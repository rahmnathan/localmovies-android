package com.github.rahmnathan.localmovies.app.data.remote.dto

import com.github.rahmnathan.localmovies.app.media.data.Media
import com.github.rahmnathan.localmovies.app.media.data.MediaUser
import com.github.rahmnathan.localmovies.app.media.data.MediaView
import com.github.rahmnathan.localmovies.app.media.data.SignedUrls
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
    val media: MediaDto?,

    @SerializedName("mediaViews")
    val mediaViews: List<MediaViewDto>?,

    @SerializedName("signedUrls")
    val signedUrls: SignedUrlsDto?
) {
    fun toMedia(serverUrl: String): Media {
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
            streamable = streamable ?: false,
            mediaViews = mediaViews?.map { it.toMediaView() },

            // Map signedUrls and prepend server URL if needed
            signedUrls = signedUrls?.let {
                fun makeAbsoluteUrl(url: String?): String? {
                    if (url == null) return null
                    return when {
                        url.startsWith("http") -> url
                        url.startsWith("/") -> "$serverUrl$url"
                        else -> "$serverUrl/$url"
                    }
                }

                SignedUrls(
                    poster = makeAbsoluteUrl(it.poster),
                    stream = makeAbsoluteUrl(it.stream),
                    updatePosition = makeAbsoluteUrl(it.updatePosition)
                )
            }
        )
    }
}

data class SignedUrlsDto(
    @SerializedName("stream")
    val stream: String? = null,

    @SerializedName("poster")
    val poster: String? = null,

    @SerializedName("updatePosition")
    val updatePosition: String? = null
)

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

data class MediaUserDto(
    @SerializedName("id")
    val id: Long,

    @SerializedName("userId")
    val userId: String,

    @SerializedName("created")
    val created: String,

    @SerializedName("updated")
    val updated: String
) {
    fun toMediaUser(): MediaUser {
        return MediaUser(
            id = id,
            userId = userId,
            created = created.toLongOrNull() ?: 0,
            updated = updated.toLongOrNull() ?: 0
        )
    }
}

data class MediaViewDto(
    @SerializedName("id")
    val id: Long,

    @SerializedName("position")
    val position: Double,

    @SerializedName("mediaUser")
    val mediaUser: MediaUserDto,

    @SerializedName("created")
    val created: String,

    @SerializedName("updated")
    val updated: String
) {
    fun toMediaView(): MediaView {
        return MediaView(
            id = id,
            position = position,
            mediaUser = mediaUser.toMediaUser(),
            created = created.toLongOrNull() ?: 0,
            updated = updated.toLongOrNull() ?: 0
        )
    }
}
