package com.github.rahmnathan.localmovies.app.activity.main

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.github.rahmnathan.localmovies.app.ui.LocalMoviesApp
import com.github.rahmnathan.localmovies.app.ui.theme.LocalMoviesTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main entry point for the LocalMovies Android app.
 *
 * This activity has been migrated to Jetpack Compose with modern architecture:
 * - Hilt for dependency injection (replaces manual Dagger injection)
 * - Compose UI (replaces XML layouts)
 * - ViewModels with StateFlow (replaces manual state management)
 * - Kotlin Coroutines (replaces CompletableFuture)
 *
 * Critical fixes implemented:
 * - Removed StrictMode.permitAll() - all network calls now on background threads
 * - Proper lifecycle management - no more manual threading
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Note: StrictMode.permitAll() has been REMOVED!
        // All network operations now properly use coroutines on IO dispatcher.

        setContent {
            LocalMoviesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LocalMoviesApp()
                }
            }
        }
    }
}
