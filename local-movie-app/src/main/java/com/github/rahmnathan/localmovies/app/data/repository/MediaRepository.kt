package com.github.rahmnathan.localmovies.app.data.repository

import com.github.rahmnathan.localmovies.app.data.local.UserPreferencesDataStore
import com.github.rahmnathan.localmovies.app.data.remote.MediaApi
import com.github.rahmnathan.localmovies.app.media.data.Media
import com.github.rahmnathan.localmovies.app.media.data.MediaEndpoint
import com.github.rahmnathan.localmovies.app.media.data.MediaEvent
import com.github.rahmnathan.localmovies.app.media.data.SignedUrls
import com.github.rahmnathan.localmovies.app.persistence.media.MediaPersistenceService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

sealed class Result<out T> {
    data class Success<T>(val data: T, val totalCount: Long = 0) : Result<T>()
    data class Error(val exception: Throwable, val message: String = exception.message ?: "Unknown error") : Result<Nothing>()
    object Loading : Result<Nothing>()
}

@Singleton
class MediaRepository @Inject constructor(
    private val mediaApi: MediaApi,
    private val preferencesDataStore: UserPreferencesDataStore
) {
    suspend fun getMediaList(
        path: String,
        endpoint: MediaEndpoint,
        page: Int = 0,
        size: Int = 50,
        order: String = "added",
        searchQuery: String? = null,
        genre: String? = null,
        type: String? = null
    ): Flow<Result<List<Media>>> = flow {
        emit(Result.Loading)

        try {
            // Fetch from network (no caching)
            val response = mediaApi.getMediaList(
                path = path,
                endpoint = endpoint,
                page = page,
                size = size,
                order = order,
                searchQuery = searchQuery,
                genre = genre,
                type = type
            )

            emit(Result.Success(response.mediaList, totalCount = response.totalCount))
        } catch (e: Exception) {
            emit(Result.Error(e))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getSignedUrls(mediaId: String): Result<SignedUrls> = withContext(Dispatchers.IO) {
        try {
            Result.Success(mediaApi.getSignedUrls(mediaId))
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun saveProgress(
        updatePositionUrl: String,
        position: Long
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            mediaApi.saveProgress(updatePositionUrl, position)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getMovieEvents(
        page: Int,
        size: Int
    ): Result<List<MediaEvent>> = withContext(Dispatchers.IO) {
        try {
            val timestamp = preferencesDataStore.lastUpdateFlow.first()
            val events = mediaApi.getMovieEvents(page, size, timestamp)
            Result.Success(events)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getMovieEventCount(): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val timestamp = preferencesDataStore.lastUpdateFlow.first()
            val count = mediaApi.getMovieEventCount(timestamp)
            Result.Success(count)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
