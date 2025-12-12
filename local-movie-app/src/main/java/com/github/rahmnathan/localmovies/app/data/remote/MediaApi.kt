package com.github.rahmnathan.localmovies.app.data.remote

import com.github.rahmnathan.localmovies.app.data.local.UserPreferencesDataStore
import com.github.rahmnathan.localmovies.app.data.remote.dto.MediaResponseDto
import com.github.rahmnathan.localmovies.app.media.data.Media
import com.github.rahmnathan.localmovies.app.media.data.MediaRequest
import com.github.rahmnathan.localmovies.app.media.data.SignedUrls
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaApi @Inject constructor(
    private val apiClient: ApiClient,
    private val preferencesDataStore: UserPreferencesDataStore
) {
    private suspend fun getServerUrl(): String {
        return preferencesDataStore.userCredentialsFlow.first().serverUrl
    }

    data class MediaListResponse(
        val mediaList: List<Media>,
        val totalCount: Long
    )

    suspend fun getMediaList(
        path: String?,
        page: Int,
        size: Int,
        order: String = "added",
        client: String = "ANDROID",
        searchQuery: String? = null,
        genre: String? = null,
        type: String? = null
    ): MediaListResponse = withContext(Dispatchers.IO) {
        val serverUrl = getServerUrl()
        val response = apiClient.httpClient.post("${serverUrl}/localmovie/mobile/v1/media") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(MediaRequest(
                page = page,
                pageSize = size,
                path = path,
                order = order,
                client = client,
                q = searchQuery,
                genre = genre,
                type = type
            ))
        }

        // Get total count from response header
        val totalCount = response.headers["Count"]?.toLongOrNull() ?: 0L
        android.util.Log.d("MediaApi", "Total count from header: $totalCount")

        // Use Ktor's built-in Gson deserialization
        val dtoList = response.body<List<MediaResponseDto>>()

        android.util.Log.d("MediaApi", "Received ${dtoList.size} DTOs from server (page $page)")

        val mediaList = dtoList.map { dto ->
            android.util.Log.d("MediaApi", "Mapping: fileName=${dto.fileName}, media.title=${dto.media?.title}")
            dto.toMedia()
        }

        android.util.Log.d("MediaApi", "Mapped to ${mediaList.size} Media objects")
        MediaListResponse(mediaList, totalCount)
    }

    suspend fun getSignedUrls(mediaId: String): SignedUrls = withContext(Dispatchers.IO) {
        val serverUrl = getServerUrl()
        val response = apiClient.httpClient.get("$serverUrl/localmovie/v1/media/$mediaId/url/signed")
        val signedUrls = response.body<SignedUrls>()

        // API returns relative paths - prepend server URL to make them absolute
        SignedUrls(
            stream = if (signedUrls.stream.startsWith("http")) signedUrls.stream else "$serverUrl${signedUrls.stream}",
            poster = if (signedUrls.poster.startsWith("http")) signedUrls.poster else "$serverUrl${signedUrls.poster}",
            updatePosition = if (signedUrls.updatePosition.startsWith("http")) signedUrls.updatePosition else "$serverUrl${signedUrls.updatePosition}"
        )
    }

    suspend fun saveProgress(signedUrl: String, position: Long) = withContext(Dispatchers.IO) {
        // signedUrl is now a full URL (e.g., https://server.com/path?query)
        // Extract the path and query, then append position
        val parts = signedUrl.split("?")
        val baseUrl = parts[0]
        val query = parts.getOrNull(1) ?: ""

        // If baseUrl is already a full URL, use it directly; otherwise prepend server URL
        val fullUrl = if (baseUrl.startsWith("http")) {
            "$baseUrl/$position?$query"
        } else {
            val serverUrl = getServerUrl()
            "$serverUrl$baseUrl/$position?$query"
        }

        apiClient.httpClient.patch(fullUrl)
    }

    suspend fun getNextEpisode(currentMediaId: String): Media? = withContext(Dispatchers.IO) {
        try {
            val serverUrl = getServerUrl()
            val response = apiClient.httpClient.get("$serverUrl/localmovie/v1/media/$currentMediaId/next")
            val dto = response.body<MediaResponseDto>()
            dto.toMedia()
        } catch (e: Exception) {
            android.util.Log.w("MediaApi", "No next episode found for $currentMediaId: ${e.message}")
            null
        }
    }
}
