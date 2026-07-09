package com.simple.tvboxmobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.simple.tvboxmobile.ui.detail.DetailScreen
import com.simple.tvboxmobile.ui.history.HistoryScreen
import com.simple.tvboxmobile.ui.home.HomeScreen
import com.simple.tvboxmobile.ui.nav.Screen
import com.simple.tvboxmobile.ui.player.PlayerScreen
import com.simple.tvboxmobile.ui.search.SearchScreen
import com.simple.tvboxmobile.ui.settings.SettingsScreen
import com.simple.tvboxmobile.ui.theme.TVBoxMobileTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TVBoxMobileTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNav()
                }
            }
        }
    }
}

@Composable
private fun AppNav() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                onSettingsClick = { nav.navigate(Screen.Settings.route) },
                onSearchClick = { nav.navigate(Screen.searchRoute()) },
                onHistoryClick = { nav.navigate(Screen.History.route) },
                onDoubanClick = { title ->
                    nav.navigate(Screen.searchRoute(title))
                }
            )
        }
        composable(
            Screen.Search.route,
            arguments = listOf(
                navArgument("query") {
                    type = NavType.StringType
                    defaultValue = ""
                    nullable = true
                }
            )
        ) { entry ->
            val query = entry.arguments?.getString("query") ?: ""
            val decoded = if (query.isNotBlank()) {
                runCatching { java.net.URLDecoder.decode(query, "UTF-8") }.getOrDefault(query)
            } else ""
            SearchScreen(
                onBack = { nav.popBackStack() },
                onPlayItem = { site, video ->
                    nav.navigate(Screen.Detail.route(video.id, site.key, video.title))
                },
                initialQuery = decoded
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { nav.popBackStack() })
        }
        composable(Screen.History.route) {
            HistoryScreen(
                onBack = { nav.popBackStack() },
                onResume = { item ->
                    nav.navigate(Screen.Player.route(item.videoId ?: "", item.siteKey, item.title, item.episodeUrl))
                }
            )
        }
        composable(
            Screen.Detail.route,
            arguments = listOf(
                navArgument("videoId") { type = NavType.StringType },
                navArgument("siteKey") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType }
            )
        ) { entry ->
            val videoId = java.net.URLDecoder.decode(entry.arguments?.getString("videoId") ?: "", "UTF-8")
            val siteKey = java.net.URLDecoder.decode(entry.arguments?.getString("siteKey") ?: "", "UTF-8")
            val title = java.net.URLDecoder.decode(entry.arguments?.getString("title") ?: "", "UTF-8")
            DetailScreen(
                siteKey = siteKey,
                videoId = videoId,
                title = title,
                onPlayEpisode = { epUrl, epTitle ->
                    nav.navigate(Screen.Player.route(videoId, siteKey, epTitle, epUrl))
                },
                onBack = { nav.popBackStack() }
            )
        }
        composable(
            Screen.Player.route,
            arguments = listOf(
                navArgument("videoId") { type = NavType.StringType },
                navArgument("siteKey") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType },
                navArgument("episodeUrl") { type = NavType.StringType }
            )
        ) { entry ->
            val videoId = java.net.URLDecoder.decode(entry.arguments?.getString("videoId") ?: "", "UTF-8")
            val siteKey = java.net.URLDecoder.decode(entry.arguments?.getString("siteKey") ?: "", "UTF-8")
            val title = java.net.URLDecoder.decode(entry.arguments?.getString("title") ?: "", "UTF-8")
            val episodeUrl = java.net.URLDecoder.decode(entry.arguments?.getString("episodeUrl") ?: "", "UTF-8")
            PlayerScreen(
                videoId = videoId,
                siteKey = siteKey,
                title = title,
                episodeUrl = episodeUrl,
                onBack = { nav.popBackStack() }
            )
        }
    }
}
