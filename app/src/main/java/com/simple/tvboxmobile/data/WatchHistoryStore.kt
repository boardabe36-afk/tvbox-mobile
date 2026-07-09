package com.simple.tvboxmobile.data

import android.content.Context
import android.content.SharedPreferences
import com.simple.tvbox.model.WatchHistoryItem
import org.json.JSONArray
import org.json.JSONObject

object WatchHistoryStore {
    private lateinit var prefs: SharedPreferences
    private const val KEY = "watch_history"
    private const val MAX = 50

    fun init(ctx: Context) {
        prefs = ctx.applicationContext.getSharedPreferences("tvbox_history", Context.MODE_PRIVATE)
    }

    fun all(): List<WatchHistoryItem> {
        val raw = prefs.getString(KEY, "[]") ?: "[]"
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                WatchHistoryItem(
                    key = o.optString("key"),
                    title = o.optString("title"),
                    subtitle = o.optString("subtitle").ifBlank { null },
                    siteKey = o.optString("siteKey"),
                    sourceUrl = o.optString("sourceUrl"),
                    videoId = o.optString("videoId").ifBlank { null },
                    episodeUrl = o.optString("episodeUrl"),
                    resolvedUrl = o.optString("resolvedUrl").ifBlank { null },
                    positionMs = o.optLong("positionMs"),
                    durationMs = o.optLong("durationMs"),
                    updatedAt = o.optLong("updatedAt")
                )
            }.sortedByDescending { it.updatedAt }
        }.getOrDefault(emptyList())
    }

    fun save(item: WatchHistoryItem) {
        val list = all().toMutableList()
        list.removeAll { it.key == item.key }
        list.add(0, item)
        if (list.size > MAX) list.subList(MAX, list.size).clear()
        val arr = JSONArray()
        list.forEach { h ->
            arr.put(JSONObject().apply {
                put("key", h.key)
                put("title", h.title)
                put("subtitle", h.subtitle ?: "")
                put("siteKey", h.siteKey)
                put("sourceUrl", h.sourceUrl)
                put("videoId", h.videoId ?: "")
                put("episodeUrl", h.episodeUrl)
                put("resolvedUrl", h.resolvedUrl ?: "")
                put("positionMs", h.positionMs)
                put("durationMs", h.durationMs)
                put("updatedAt", h.updatedAt)
            })
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    fun remove(key: String) {
        val list = all().toMutableList()
        list.removeAll { it.key == key }
        val arr = JSONArray()
        list.forEach { h ->
            arr.put(JSONObject().apply {
                put("key", h.key)
                put("title", h.title)
                put("subtitle", h.subtitle ?: "")
                put("siteKey", h.siteKey)
                put("sourceUrl", h.sourceUrl)
                put("videoId", h.videoId ?: "")
                put("episodeUrl", h.episodeUrl)
                put("resolvedUrl", h.resolvedUrl ?: "")
                put("positionMs", h.positionMs)
                put("durationMs", h.durationMs)
                put("updatedAt", h.updatedAt)
            })
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY).apply()
    }

    fun find(key: String): WatchHistoryItem? = all().find { it.key == key }
}
