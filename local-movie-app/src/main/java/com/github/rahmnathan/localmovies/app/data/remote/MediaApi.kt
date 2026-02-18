package com.github.rahmnathan.localmovies.app.data.remote

import com.github.rahmnathan.localmovies.app.data.local.UserPreferencesDataStore
import com.github.rahmnathan.localmovies.app.di.DynamicOAuth2Service
import com.github.rahmnathan.localmovies.app.media.data.Media
import com.github.rahmnathan.localmovies.app.media.data.MediaUser
import com.github.rahmnathan.localmovies.app.media.data.MediaView
import com.github.rahmnathan.localmovies.app.media.data.ParentMedia
import com.github.rahmnathan.localmovies.app.media.data.Recommendation as AppRecommendation
import com.github.rahmnathan.localmovies.app.media.data.SignedUrls
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.openapitools.client.ApiClient as GeneratedApiClient
import org.openapitools.client.JSON
import org.openapitools.client.api.MediaResourceApi
import org.openapitools.client.model.MediaFileDto
import org.openapitools.client.model.MediaRequest
import org.openapitools.client.model.ParentMediaDto
import java.lang.reflect.Type
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaApi @Inject constructor(
    private val preferencesDataStore: UserPreferencesDataStore,
    private val dynamicOAuth2Service: DynamicOAuth2Service
) {
    companion object {
        private const val TAG = "MediaApi"
    }

    init {
        configureGeneratedJsonDateParsing()
    }

    private suspend fun getServerUrl(): String {
        return preferencesDataStore.userCredentialsFlow.first().serverUrl
    }

    private suspend fun createAuthorizedOkHttpClient(): OkHttpClient {
        val accessToken = dynamicOAuth2Service.getServiceSuspend().accessToken.serialize()
        return OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("x-correlation-id", java.util.UUID.randomUUID().toString())
                    .header("Authorization", "Bearer $accessToken")
                    .build()
                chain.proceed(request)
            })
            .build()
    }

    private fun configureGeneratedJsonDateParsing() {
        val epochOrIsoOffsetAdapter = object : JsonSerializer<OffsetDateTime>, JsonDeserializer<OffsetDateTime> {
            override fun serialize(src: OffsetDateTime?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
                return if (src == null) JsonNull.INSTANCE else JsonPrimitive(src.toString())
            }

            override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): OffsetDateTime? {
                if (json == null || json.isJsonNull) return null
                val primitive = json.asJsonPrimitive
                if (primitive.isNumber) {
                    return OffsetDateTime.ofInstant(Instant.ofEpochSecond(primitive.asLong), ZoneOffset.UTC)
                }
                val text = primitive.asString?.trim().orEmpty()
                if (text.isEmpty()) return null
                return try {
                    // Epoch seconds as string
                    OffsetDateTime.ofInstant(Instant.ofEpochSecond(text.toLong()), ZoneOffset.UTC)
                } catch (_: Exception) {
                    try {
                        // ISO with offset
                        OffsetDateTime.parse(text)
                    } catch (_: Exception) {
                        // ISO local datetime without offset
                        LocalDateTime.parse(text).atOffset(ZoneOffset.UTC)
                    }
                }
            }
        }

        val gson = JSON.createGson()
            .registerTypeAdapter(OffsetDateTime::class.java, epochOrIsoOffsetAdapter)
            .create()
        JSON.setGson(gson)
    }

    private suspend fun createGeneratedMediaApi(serverUrl: String): MediaResourceApi {
        val generatedApiClient = GeneratedApiClient(createAuthorizedOkHttpClient())
            .setBasePath(serverUrl)
        return MediaResourceApi(generatedApiClient)
    }

    data class MediaListResponse(
        val mediaList: List<Media>,
        val totalCount: Long
    )

    suspend fun getMediaList(
        parentId: String? = null,
        page: Int,
        size: Int,
        order: String = "added",
        client: String = "ANDROID",
        searchQuery: String? = null,
        genre: String? = null,
        type: String? = null
    ): MediaListResponse = withContext(Dispatchers.IO) {
        val serverUrl = getServerUrl()
        val mediaApi = createGeneratedMediaApi(serverUrl)
        val generatedRequest = MediaRequest()
            .page(page)
            .pageSize(size)
            .order(order)
            .path(null)

        if (parentId != null) generatedRequest.parentId(parentId)
        if (searchQuery != null) generatedRequest.q(searchQuery)
        if (genre != null) generatedRequest.genre(genre)
        if (type != null) generatedRequest.type(type)

        val response = mediaApi.getMediaWithHttpInfo(generatedRequest)
        val responseData = response.data ?: emptyList()

        val totalCount = response.headers.entries
            .firstOrNull { it.key.equals("Count", ignoreCase = true) }
            ?.value
            ?.firstOrNull()
            ?.toLongOrNull()
            ?: 0L
        android.util.Log.d(TAG, "Total count from header: $totalCount")
        android.util.Log.d(TAG, "Received ${responseData.size} media DTOs from server (page $page)")

        val mediaList = responseData.map { it.toAppMedia(serverUrl) }
        android.util.Log.d(TAG, "Mapped to ${mediaList.size} Media objects")

        MediaListResponse(mediaList, totalCount)
    }

    suspend fun getSignedUrls(mediaId: String): SignedUrls = withContext(Dispatchers.IO) {
        val serverUrl = getServerUrl()
        val signedUrls = createGeneratedMediaApi(serverUrl).getSignedUrls(mediaId)

        fun makeAbsoluteUrl(url: String?): String? {
            if (url == null) return null
            return when {
                url.startsWith("http") -> url
                url.startsWith("/") -> "$serverUrl$url"
                else -> "$serverUrl/$url"
            }
        }

        SignedUrls(
            stream = makeAbsoluteUrl(signedUrls.stream) ?: throw IllegalStateException("Expected non-null stream URL"),
            poster = makeAbsoluteUrl("/localmovie/v1/signed/media/$mediaId/poster"),
            updatePosition = makeAbsoluteUrl(signedUrls.updatePosition) ?: throw IllegalStateException("Expected non-null updatePosition URL"),
            subtitle = makeAbsoluteUrl(signedUrls.subtitle)
        )
    }

    suspend fun saveProgress(signedUrl: String, position: Long, duration: Long? = null) = withContext(Dispatchers.IO) {
        val parts = signedUrl.split("?")
        val baseUrl = parts[0]
        val query = parts.getOrNull(1) ?: ""
        val positionSeconds = position / 1000

        val queryWithDuration = if (duration != null && duration > 0) {
            if (query.isNotEmpty()) "$query&duration=$duration" else "duration=$duration"
        } else {
            query
        }

        val fullUrl = if (baseUrl.startsWith("http")) {
            "$baseUrl/$positionSeconds?$queryWithDuration"
        } else {
            val serverUrl = getServerUrl()
            "$serverUrl$baseUrl/$positionSeconds?$queryWithDuration"
        }

        val request = Request.Builder()
            .url(fullUrl)
            .patch("".toRequestBody(null))
            .build()

        createAuthorizedOkHttpClient().newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Failed to save progress: HTTP ${response.code}")
            }
        }
    }

    suspend fun addFavorite(mediaFileId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val serverUrl = getServerUrl()
            createGeneratedMediaApi(serverUrl).addFavorite(mediaFileId)
            true
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error adding favorite", e)
            false
        }
    }

    suspend fun removeFavorite(mediaFileId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val serverUrl = getServerUrl()
            createGeneratedMediaApi(serverUrl).removeFavorite(mediaFileId)
            true
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error removing favorite", e)
            false
        }
    }

    suspend fun removeFromHistory(mediaFileId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val serverUrl = getServerUrl()
            createGeneratedMediaApi(serverUrl).removeFromHistory(mediaFileId)
            true
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error removing from history", e)
            false
        }
    }

    suspend fun getRecommendations(): List<AppRecommendation> = withContext(Dispatchers.IO) {
        try {
            val serverUrl = getServerUrl()
            val response = createGeneratedMediaApi(serverUrl).getRecommendations()
            android.util.Log.d(TAG, "Received ${response.size} recommendations")

            response.mapNotNull { rec ->
                val mediaFile = rec.mediaFile ?: return@mapNotNull null
                AppRecommendation(
                    media = mediaFile.toAppMedia(serverUrl),
                    reason = rec.reason,
                    rank = rec.rank ?: 0
                )
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error fetching recommendations", e)
            emptyList()
        }
    }

    private fun MediaFileDto.toAppMedia(serverUrl: String): Media {
        val media = this.media
        return Media(
            title = media?.title ?: fileName ?: "",
            imdbRating = media?.imdbRating,
            metaRating = media?.metaRating,
            image = null,
            releaseYear = media?.releaseYear,
            created = created?.toEpochSecond(),
            genre = media?.genre,
            filename = fileName ?: "",
            actors = media?.actors,
            plot = media?.plot,
            path = path ?: "",
            number = media?.number?.toString(),
            type = mediaFileType?.name ?: "DIRECTORY",
            mediaFileId = mediaFileId ?: "",
            streamable = streamable ?: false,
            mediaViews = mediaViews?.map { it.toAppMediaView() },
            signedUrls = SignedUrls(
                poster = "$serverUrl/localmovie/v1/signed/media/${mediaFileId ?: ""}/poster",
                stream = "",
                updatePosition = ""
            ),
            parent = parent.toAppParentMedia(),
            favorite = favorite ?: false
        )
    }

    private fun org.openapitools.client.model.MediaViewDto.toAppMediaView(): MediaView {
        val user = mediaUser ?: return MediaView(
            id = id ?: 0L,
            position = position ?: 0.0,
            duration = duration,
            mediaUser = MediaUser(0L, "", 0L, 0L),
            created = created?.toEpochSecond() ?: 0L,
            updated = updated?.toEpochSecond() ?: 0L
        )

        return MediaView(
            id = id ?: 0L,
            position = position ?: 0.0,
            duration = duration,
            mediaUser = MediaUser(
                id = user.id ?: 0L,
                userId = user.userId ?: "",
                created = user.created?.toEpochSecond() ?: 0L,
                updated = user.updated?.toEpochSecond() ?: 0L
            ),
            created = created?.toEpochSecond() ?: 0L,
            updated = updated?.toEpochSecond() ?: 0L
        )
    }

    private fun ParentMediaDto?.toAppParentMedia(): ParentMedia? {
        this ?: return null
        return ParentMedia(
            mediaFileId = mediaFileId ?: "",
            mediaFileType = mediaFileType?.name ?: "",
            title = title,
            number = number,
            parent = toNestedParent(parent)
        )
    }

    private fun toNestedParent(rawParent: Any?): ParentMedia? {
        val next = rawParent as? ParentMediaDto ?: return null
        return next.toAppParentMedia()
    }
}
