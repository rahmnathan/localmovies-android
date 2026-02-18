package com.github.rahmnathan.localmovies.app.cast.config

import android.content.Context
import com.google.android.gms.cast.CastMediaControlIntent
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider

/**
 * Provides configuration options for the Google Cast framework.
 * This class is referenced in AndroidManifest.xml and is required for Cast to function.
 */
class CastOptionsProvider : OptionsProvider {

    companion object {
        /**
         * Custom Cast receiver app ID registered at cast.google.com/publish
         * The receiver is hosted at: https://movies.nathanrahm.com/cast/receiver.html
         */
        private const val CAST_APP_ID = "9A05279D"

        // Set to true to use custom receiver, false to use default (for testing)
        private const val USE_CUSTOM_RECEIVER = true
    }

    override fun getCastOptions(context: Context): CastOptions {
        val appId = if (USE_CUSTOM_RECEIVER) {
            CAST_APP_ID
        } else {
            CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID
        }

        return CastOptions.Builder()
            .setReceiverApplicationId(appId)
            .setStopReceiverApplicationWhenEndingSession(true)
            .build()
    }

    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider>? {
        return null
    }
}
