package com.github.rahmnathan.localmovies.app.data.remote.dto

import com.github.rahmnathan.localmovies.app.media.data.Media
import com.github.rahmnathan.localmovies.app.media.data.MediaUser
import com.github.rahmnathan.localmovies.app.media.data.MediaView
import com.github.rahmnathan.localmovies.app.media.data.ParentMedia
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

    @SerializedName("mediaFileType")
    val mediaFileType: String?,

    @SerializedName("streamable")
    val streamable: Boolean?,

    @SerializedName("created")
    val created: String?,  // API returns as string timestamp

    @SerializedName("media")
    val media: MediaDto?,

    @SerializedName("mediaViews")
    val mediaViews: List<MediaViewDto>?,

    @SerializedName("signedUrls")
    val signedUrls: SignedUrlsDto?,

    @SerializedName("parent")
    val parent: ParentMediaDto?,

    @SerializedName("favorite")
    val favorite: Boolean?
) {
    fun toMedia(serverUrl: String): Media {
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
            type = mediaFileType ?: "DIRECTORY",
            mediaFileId = mediaFileId,
            streamable = streamable ?: false,
            mediaViews = mediaViews?.map { it.toMediaView() },
            signedUrls = SignedUrls(
                poster = "$serverUrl/localmovie/v1/signed/media/$mediaFileId/poster",
                stream = "",
                updatePosition = ""
            ),
            parent = parent?.toParentMedia(),
            favorite = favorite ?: false
        )
    }
}

data class ParentMediaDto(
    @SerializedName("mediaFileId")
    val mediaFileId: String?,

    @SerializedName("mediaFileType")
    val mediaFileType: String?,

    @SerializedName("title")
    val title: String?,

    @SerializedName("number")
    val number: Int?,

    @SerializedName("parent")
    val parent: ParentMediaDto?
) {
    fun toParentMedia(): ParentMedia {
        return ParentMedia(
            mediaFileId = mediaFileId ?: "",
            mediaFileType = mediaFileType ?: "",
            title = title,
            number = number,
            parent = parent?.toParentMedia()
        )
    }
}

data class SignedUrlsDto(
    @SerializedName("stream")
    val stream: String? = null,

    @SerializedName("poster")
    val poster: String? = null,

    @SerializedName("updatePosition")
    val updatePosition: String? = null,

    @SerializedName("subtitle")
    val subtitle: String? = null
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
            created = parseDateTime(created),
            updated = parseDateTime(updated)
        )
    }
}

data class MediaViewDto(
    @SerializedName("id")
    val id: Long,

    @SerializedName("position")
    val position: Double,

    @SerializedName("duration")
    val duration: Double?,

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
            duration = duration,
            mediaUser = mediaUser.toMediaUser(),
            created = parseDateTime(created),
            updated = parseDateTime(updated)
        )
    }
}

/**
 * Parse ISO 8601 datetime string to epoch seconds.
 * Handles formats like "2026-02-15T19:00:00" or "2026-02-15T19:00:00.123"
 */
private fun parseDateTime(dateTimeStr: String): Long {
    return try {
        java.time.LocalDateTime.parse(dateTimeStr)
            .atZone(java.time.ZoneId.systemDefault())
            .toEpochSecond()
    } catch (e: Exception) {
        // Fallback: try parsing as epoch millis or return 0
        dateTimeStr.toLongOrNull() ?: 0
    }
}
