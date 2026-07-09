package com.simple.tvboxmobile.ui.nav

import java.net.URLEncoder

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Search : Screen("search?query={query}")
    object Settings : Screen("settings")
    object History : Screen("history")

    object Detail : Screen("detail/{videoId}&{siteKey}&{title}") {
        fun route(videoId: String, siteKey: String, title: String): String =
            "detail/${URLEncoder.encode(videoId, "UTF-8")}&" +
                "${URLEncoder.encode(siteKey, "UTF-8")}&" +
                "${URLEncoder.encode(title, "UTF-8")}"
    }

    object Player : Screen("player/{videoId}&{siteKey}&{title}&{episodeUrl}") {
        fun route(videoId: String, siteKey: String, title: String, episodeUrl: String): String =
            "player/${URLEncoder.encode(videoId, "UTF-8")}&" +
                "${URLEncoder.encode(siteKey, "UTF-8")}&" +
                "${URLEncoder.encode(title, "UTF-8")}&" +
                "${URLEncoder.encode(episodeUrl, "UTF-8")}"
    }

    companion object {
        fun searchRoute(query: String = ""): String =
            if (query.isBlank()) "search?query=" else "search?query=${URLEncoder.encode(query, "UTF-8")}"
    }
}
