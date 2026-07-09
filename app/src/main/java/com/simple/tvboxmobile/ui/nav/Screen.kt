package com.simple.tvboxmobile.ui.nav

/**
 * 路由表。值是 route 字符串。
 */
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Search : Screen("search")
    object Settings : Screen("settings")
    object Player : Screen("player/{videoId}&{siteKey}&{title}") {
        fun route(videoId: String, siteKey: String, title: String): String =
            "player/${java.net.URLEncoder.encode(videoId, "UTF-8")}&" +
                "${java.net.URLEncoder.encode(siteKey, "UTF-8")}&" +
                "${java.net.URLEncoder.encode(title, "UTF-8")}"
    }
}
