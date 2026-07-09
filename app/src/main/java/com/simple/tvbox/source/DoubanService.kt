package com.simple.tvbox.source

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.simple.tvbox.model.VideoItem
import com.simple.tvbox.util.HttpUtil

/**
 * 豆瓣热门（v1 移动端未启用 UI，但保留 API 供后续 v1.1 复用）。
 */
object DoubanService {

    private val gson = Gson()

    data class DoubanItem(
        @SerializedName("title") val title: String,
        @SerializedName("url") val url: String,
        @SerializedName("rate") val rate: String?,
        @SerializedName("cover") val cover: String?
    )

    /**
     * 抓取豆瓣频道（简化版：直接用 HttpUtil 拉 JSON）。
     */
    fun fetchChannel(channel: String, page: Int = 0): List<DoubanItem> {
        val url = when (channel) {
            "movie_hot" -> "https://movie.douban.com/j/search_subjects?type=movie&tag=%E7%83%AD%E9%97%A8&page_limit=20&page_start=${page * 20}"
            "tv_hot" -> "https://movie.douban.com/j/search_subjects?type=tv&tag=%E7%83%AD%E9%97%A8&page_limit=20&page_start=${page * 20}"
            "movie_top250" -> "https://movie.douban.com/j/search_subjects?type=movie&tag=top250&page_limit=20&page_start=${page * 20}"
            else -> "https://movie.douban.com/j/search_subjects?type=movie&tag=%E7%83%AD%E9%97%A8&page_limit=20&page_start=${page * 20}"
        }
        return runCatching {
            val body = HttpUtil.fetchText(url, referer = "https://movie.douban.com/")
            val parsed = gson.fromJson(body, DoubanSubjectsResponse::class.java)
            parsed.subjects ?: emptyList()
        }.getOrDefault(emptyList())
    }

    private data class DoubanSubjectsResponse(
        @SerializedName("subjects") val subjects: List<DoubanItem>?
    )
}
