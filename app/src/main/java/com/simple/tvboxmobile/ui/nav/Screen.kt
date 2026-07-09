package com.simple.tvboxmobile.ui.nav

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Search : Screen("search")
    object Settings : Screen("settings")
    object History : Screen("history")

    object Detail : Screen("detail/{videoId}&{siteKey}&{title}") {
        fun route(videoId: String, siteKey: String, title: String): String =
            "detail/${java.net.URLEncoder.encode(videoId, "UTF-8")}&" +
                "${java.net.URLEncoder.encode(siteKey, "UTF-8")}&" +
                "${java.net.URLEncoder.encode(title, "UTF-8")}"
    }

    object Player : Screen("player/{videoId}&{siteKey}&{title}&{episodeUrl}") {
        fun route(videoId: String, siteKey: String, title: String, episodeUrl: String): String =
            "player/${java.net.URLEncoder.encode(videoId, "UTF-8")}&" +
                "${java.net.URLEncoder.encode(siteKey, "UTF-8")}&" +
                "${java.net.URLEncoder.encode(title, "UTF-8")}&" +
                "${java.net.URLEncoder.encode(episodeUrl, "UTF-8")}"
    }
}
