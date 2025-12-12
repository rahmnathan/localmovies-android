package com.github.rahmnathan.localmovies.app.ui

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.flow.first
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class Screen(val route: String) {
    object Setup : Screen("setup")
    object Main : Screen("main")
    object CastController : Screen("cast_controller")
    object Player : Screen("player/{url}/{updatePositionUrl}") {
        fun createRoute(url: String, updatePositionUrl: String): String {
            val encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
            val encodedUpdateUrl = URLEncoder.encode(updatePositionUrl, StandardCharsets.UTF_8.toString())
            return "player/$encodedUrl/$encodedUpdateUrl"
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
    val startDestination = if (uiState.username.isBlank()) {
        Screen.Setup.route
    } else {
        Screen.Main.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Setup.route) {
            SetupScreen(
                onNavigateToMain = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Setup.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Main.route) {
            MainScreen(
                onNavigateToPlayer = { url, updatePositionUrl ->
                    navController.navigate(Screen.Player.createRoute(url, updatePositionUrl))
                },
                onNavigateToCastController = {
                    navController.navigate(Screen.CastController.route)
                }
            )
        }

        composable(Screen.CastController.route) {
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
                navArgument("updatePositionUrl") { type = NavType.StringType }
            )
        ) {
            PlayerScreen()
        }

        composable(
            route = Screen.Detail.route,
            arguments = listOf(
                navArgument("mediaId") { type = NavType.StringType }
            )
        ) {
            // Detail screen is shown as a dialog from MainScreen
            Text("Detail Screen")
        }
    }
}
