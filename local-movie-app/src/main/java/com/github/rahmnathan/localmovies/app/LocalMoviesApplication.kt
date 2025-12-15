package com.github.rahmnathan.localmovies.app

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.util.DebugLogger
import com.github.rahmnathan.localmovies.app.di.DynamicOAuth2Service
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okio.Path.Companion.toOkioPath
import javax.inject.Inject

@HiltAndroidApp
class LocalMoviesApplication : Application(), SingletonImageLoader.Factory {

    @Inject
    lateinit var dynamicOAuth2Service: DynamicOAuth2Service

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        // Ensure cache directory exists before configuring Coil
        val imageCacheDir = cacheDir.resolve("image_cache")
        if (!imageCacheDir.exists()) {
            imageCacheDir.mkdirs()
        }

        // Create OkHttp client with authentication interceptor
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(dynamicOAuth2Service))
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
     * OkHttp interceptor that adds OAuth2 bearer token to image requests
     */
    private class AuthInterceptor(
        private val dynamicOAuth2Service: DynamicOAuth2Service
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()

            // Get the access token using fresh credentials
            val accessToken = runBlocking {
                try {
                    dynamicOAuth2Service.getService().accessToken.serialize()
                } catch (e: Exception) {
                    null
                }
            }

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