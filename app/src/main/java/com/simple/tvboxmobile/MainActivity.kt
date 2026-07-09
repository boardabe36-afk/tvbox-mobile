package com.simple.tvboxmobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.simple.tvboxmobile.ui.detail.DetailScreen
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
                onVideoClick = { _ -> /* TODO: 详情页 v1.1 */ },
                onSettingsClick = { nav.navigate(Screen.Settings.route) },
                onSearchClick = { nav.navigate(Screen.Search.route) }
            )
        }
        composable(Screen.Search.route) {
            SearchScreen(
                onBack = { nav.popBackStack() },
                onPlayItem = { site, video ->
                    // 搜索结果直接播放（v1 简化：单击→搜索该剧详情）
                    // TODO: v1.1 加详情页点击
                }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { nav.popBackStack() })
        }
        composable(
            Screen.Player.route,
            arguments = listOf(
                navArgument("videoId") { type = NavType.StringType },
                navArgument("siteKey") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType }
            )
        ) { entry ->
            val videoId = java.net.URLDecoder.decode(entry.arguments?.getString("videoId") ?: "", "UTF-8")
            val siteKey = java.net.URLDecoder.decode(entry.arguments?.getString("siteKey") ?: "", "UTF-8")
            val title = java.net.URLDecoder.decode(entry.arguments?.getString("title") ?: "", "UTF-8")
            PlayerScreen(videoId = videoId, siteKey = siteKey, title = title, onBack = { nav.popBackStack() })
        }
    }
}
