package com.github.rahmnathan.localmovies.app

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.util.DebugLogger
import com.github.rahmnathan.localmovies.app.auth.TokenCache
import com.github.rahmnathan.localmovies.app.cast.CastProgressTracker
import dagger.hilt.android.HiltAndroidApp
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okio.Path.Companion.toOkioPath
import javax.inject.Inject

@HiltAndroidApp
class LocalMoviesApplication : Application(), SingletonImageLoader.Factory {

    @Inject
    lateinit var tokenCache: TokenCache

    @Inject
    lateinit var castProgressTracker: CastProgressTracker

    override fun onCreate() {
        super.onCreate()
        // Initialize cast progress tracking to ensure all queued episodes are tracked
        castProgressTracker.initialize()
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        // Ensure cache directory exists before configuring Coil
        val imageCacheDir = cacheDir.resolve("image_cache")
        if (!imageCacheDir.exists()) {
            imageCacheDir.mkdirs()
        }

        // Create OkHttp client with authentication interceptor
        // Uses TokenCache which provides cached tokens without blocking
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenCache))
            .build()

        return ImageLoader.Builder(context)
            // Memory cache configuration (25% of available memory)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            // Disk cache configuration (250MB)
            .diskCache {
                DiskCache.Builder()
                    .directory(imageCacheDir.toOkioPath())
                    .maxSizeBytes(250 * 1024 * 1024) // 250MB
                    .build()
            }
            // Use authenticated OkHttp client
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient }))
            }
            // Enable logging in debug builds
            .logger(DebugLogger())
            .build()
    }

    /**
     * OkHttp interceptor that adds OAuth2 bearer token to image requests.
     * Uses TokenCache to get tokens without blocking - tokens are refreshed
     * proactively in the background when credentials change.
     */
    private class AuthInterceptor(
        private val tokenCache: TokenCache
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()

            // Get the cached access token (non-blocking)
            val accessToken = tokenCache.getAccessToken()

            // Add Authorization header and correlation ID
            val authenticatedRequest = request.newBuilder().apply {
                if (accessToken != null) {
                    addHeader("Authorization", "Bearer $accessToken")
                }
                addHeader("x-correlation-id", java.util.UUID.randomUUID().toString())
            }.build()

            return chain.proceed(authenticatedRequest)
        }
    }
}