package com.github.rahmnathan.localmovies.app.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.github.rahmnathan.localmovies.app.ui.cast.CastControllerScreen
import com.github.rahmnathan.localmovies.app.ui.main.MainScreen
import com.github.rahmnathan.localmovies.app.ui.player.PlayerScreen
import com.github.rahmnathan.localmovies.app.ui.setup.SetupScreen
import com.github.rahmnathan.localmovies.app.ui.setup.SetupViewModel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private const val TRANSITION_DURATION = 300

sealed class Screen(val route: String) {
    object Setup : Screen("setup")
    object Main : Screen("main")
    object CastController : Screen("cast_controller")
    object Player : Screen("player/{url}/{updatePositionUrl}/{mediaId}/{resumePosition}") {
        fun createRoute(url: String, updatePositionUrl: String, mediaId: String, resumePosition: Long = 0): String {
            val encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
            val encodedUpdateUrl = URLEncoder.encode(updatePositionUrl, StandardCharsets.UTF_8.toString())
            val encodedMediaId = URLEncoder.encode(mediaId, StandardCharsets.UTF_8.toString())
            return "player/$encodedUrl/$encodedUpdateUrl/$encodedMediaId/$resumePosition"
        }
    }
    object Detail : Screen("detail/{mediaId}") {
        fun createRoute(mediaId: String): String {
            return "detail/$mediaId"
        }
    }
}

@Composable
fun LocalMoviesApp() {
    // Use SetupViewModel to access credentials for initial route determination
    val setupViewModel: SetupViewModel = hiltViewModel()
    val uiState by setupViewModel.uiState.collectAsStateWithLifecycle()

    val navController = rememberNavController()

    // Determine start destination based on whether credentials exist
    // Default to Setup screen if no credentials
    val startDestination = if (uiState.username.isNotBlank()) {
        Screen.Main.route
    } else {
        Screen.Setup.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(
            route = Screen.Setup.route,
            enterTransition = { fadeIn(animationSpec = tween(TRANSITION_DURATION)) },
            exitTransition = { fadeOut(animationSpec = tween(TRANSITION_DURATION)) }
        ) {
            SetupScreen(
                onNavigateToMain = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Setup.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.Main.route,
            enterTransition = { fadeIn(animationSpec = tween(TRANSITION_DURATION)) },
            exitTransition = { fadeOut(animationSpec = tween(TRANSITION_DURATION)) },
            popEnterTransition = { fadeIn(animationSpec = tween(TRANSITION_DURATION)) },
            popExitTransition = { fadeOut(animationSpec = tween(TRANSITION_DURATION)) }
        ) {
            MainScreen(
                onNavigateToPlayer = { url, updatePositionUrl, mediaId, resumePosition ->
                    navController.navigate(Screen.Player.createRoute(url, updatePositionUrl, mediaId, resumePosition))
                },
                onNavigateToCastController = {
                    navController.navigate(Screen.CastController.route)
                },
                onNavigateToSetup = {
                    navController.navigate(Screen.Setup.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.CastController.route,
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(TRANSITION_DURATION)
                ) + fadeIn(animationSpec = tween(TRANSITION_DURATION))
            },
            exitTransition = { fadeOut(animationSpec = tween(TRANSITION_DURATION)) },
            popEnterTransition = { fadeIn(animationSpec = tween(TRANSITION_DURATION)) },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(TRANSITION_DURATION)
                ) + fadeOut(animationSpec = tween(TRANSITION_DURATION))
            }
        ) {
            CastControllerScreen(
                onDismiss = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.Player.route,
            arguments = listOf(
                navArgument("url") { type = NavType.StringType },
                navArgument("updatePositionUrl") { type = NavType.StringType },
                navArgument("mediaId") { type = NavType.StringType },
                navArgument("resumePosition") { type = NavType.LongType }
            ),
            enterTransition = { fadeIn(animationSpec = tween(TRANSITION_DURATION)) },
            exitTransition = { fadeOut(animationSpec = tween(TRANSITION_DURATION)) },
            popEnterTransition = { fadeIn(animationSpec = tween(TRANSITION_DURATION)) },
            popExitTransition = { fadeOut(animationSpec = tween(TRANSITION_DURATION)) }
        ) {
            PlayerScreen(
                onNavigateToNextEpisode = { url, updatePositionUrl, mediaId, resumePosition ->
                    navController.navigate(Screen.Player.createRoute(url, updatePositionUrl, mediaId, resumePosition)) {
                        popUpTo(Screen.Player.route) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.Detail.route,
            arguments = listOf(
                navArgument("mediaId") { type = NavType.StringType }
            ),
            enterTransition = { fadeIn(animationSpec = tween(TRANSITION_DURATION)) },
            exitTransition = { fadeOut(animationSpec = tween(TRANSITION_DURATION)) }
        ) {
            // Detail screen is shown as a dialog from MainScreen
            Text("Detail Screen")
        }
    }
}
