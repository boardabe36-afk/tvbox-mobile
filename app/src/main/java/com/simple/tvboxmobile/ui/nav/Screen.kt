package com.simple.tvboxmobile.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import java.net.URLEncoder

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Settings : Screen("settings")
    object History : Screen("history")

    object Search : Screen("search?query={query}") {
        fun searchRoute(query: String = ""): String {
            val encoded = if (query.isNotBlank()) URLEncoder.encode(query, "UTF-8") else ""
            return if (encoded.isNotBlank()) "search?query=$encoded" else "search?query="
        }
    }

    object Detail : Screen("detail/{videoId}&{siteKey}&{title}") {
        fun route(videoId: String, siteKey: String, title: String): String {
            return "detail/${URLEncoder.encode(videoId, "UTF-8")}&${URLEncoder.encode(siteKey, "UTF-8")}&${URLEncoder.encode(title, "UTF-8")}"
        }
    }

    object Player : Screen("player/{videoId}&{siteKey}&{title}&{episodeUrl}") {
        fun route(videoId: String, siteKey: String, title: String, episodeUrl: String): String {
            return "player/${URLEncoder.encode(videoId, "UTF-8")}&${URLEncoder.encode(siteKey, "UTF-8")}&${URLEncoder.encode(title, "UTF-8")}&${URLEncoder.encode(episodeUrl, "UTF-8")}"
        }
    }

    object Category : Screen("category/{siteKey}&{categoryId}&{categoryName}") {
        fun route(siteKey: String, categoryId: String, categoryName: String): String {
            return "category/${URLEncoder.encode(siteKey, "UTF-8")}&${URLEncoder.encode(categoryId, "UTF-8")}&${URLEncoder.encode(categoryName, "UTF-8")}"
        }
    }
}
