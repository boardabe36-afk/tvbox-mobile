package com.simple.tvbox.ui.detail

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.simple.tvbox.R
import com.simple.tvbox.TvBoxApp
import com.simple.tvbox.model.SpiderSite
import com.simple.tvbox.source.VideoClientFactory
import com.simple.tvbox.ui.player.PlayerActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 视频详情 + 剧集列表。
 *
 * 简版：自绘的剧集网格（避免引入更多 Leanback 类）。
 * 焦点导航：左右切换集数、确定键播放。
 */
class DetailActivity : FragmentActivity() {

    private lateinit var titleView: TextView
    private lateinit var descView: TextView
    private lateinit var episodeContainer: LinearLayout

    private var site: SpiderSite? = null
    private var videoId: String = ""
    private var episodes: List<Pair<String, String>> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        titleView = findViewById(R.id.detail_title)
        descView = findViewById(R.id.detail_desc)
        episodeContainer = findViewById(R.id.detail_episodes)

        val siteKey = intent.getStringExtra(EXTRA_SITE_KEY) ?: return finish()
        videoId = intent.getStringExtra(EXTRA_VIDEO_ID) ?: return finish()
        val title = intent.getStringExtra(EXTRA_TITLE) ?: ""
        titleView.text = title

        site = findSite(siteKey)
        if (site == null) {
            Toast.makeText(this, "未找到站点", Toast.LENGTH_LONG).show()
            return
        }
        load()
    }

    private fun findSite(key: String): SpiderSite? {
        val repo = TvBoxApp.get().sourceRepository
        return repo.getAllSources()
            .firstNotNullOfOrNull { src -> repo.findSite(src.url, key) }
    }

    private fun load() {
        val s = site ?: return
        val client = VideoClientFactory.create(s)
        lifecycleScope.launch {
            try {
                val (info, eps) = withContext(Dispatchers.IO) {
                    val detail = client.fetchDetailInfo(videoId)
                    val episodes = client.fetchEpisodes(videoId)
                    detail to episodes
                }
                descView.text = listOfNotNull(
                    info?.optString("vod_year").ifBlankOrNull(),
                    info?.optString("vod_area").ifBlankOrNull(),
                    info?.optString("vod_director").ifBlankOrNull(),
                    info?.optString("vod_actor").ifBlankOrNull()
                ).joinToString("  ·  ").ifBlank { "暂无简介" }
                episodes = eps
                renderEpisodes()
            } catch (t: Throwable) {
                Toast.makeText(this@DetailActivity, "加载失败：${t.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun String?.ifBlankOrNull(): String? =
        if (this.isNullOrBlank()) null else this

    private fun renderEpisodes() {
        episodeContainer.removeAllViews()
        episodes.forEachIndexed { idx, (name, url) ->
            val tv = TextView(this).apply {
                text = name
                textSize = 16f
                gravity = Gravity.CENTER
                isFocusable = true
                isFocusableInTouchMode = true
                isClickable = true
                setBackgroundResource(R.drawable.bg_card)
                setTextColor(Color.WHITE)
                setPadding(40, 20, 40, 20)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = 16 }
                setOnClickListener { play(url, name) }
            }
            episodeContainer.addView(tv)
        }
        if (episodes.isEmpty()) {
            val tv = TextView(this).apply {
                text = "该视频没有可播放的剧集"
                setTextColor(Color.LTGRAY)
                setPadding(40, 20, 40, 20)
            }
            episodeContainer.addView(tv)
        }
    }

    private fun play(episodeUrl: String, episodeName: String) {
        val s = site ?: return
        // 用新版 intent：传原始剧集 URL，PlayerActivity 自己 resolve
        // 这样 thisUrl 临时挂掉时点重试能重新请求
        val srcUrl = findSourceUrlForSite(s.key)
        startActivity(
            PlayerActivity.intent(
                this,
                title = titleView.text.toString(),
                subtitle = episodeName,
                siteKey = s.key,
                sourceUrl = srcUrl,
                episodeUrl = episodeUrl
            )
        )
    }

    private fun findSourceUrlForSite(siteKey: String): String {
        val repo = TvBoxApp.get().sourceRepository
        return repo.getAllSources().firstOrNull { src ->
            repo.findSite(src.url, siteKey) != null
        }?.url ?: ""
    }

    companion object {
        private const val EXTRA_SITE_KEY = "site_key"
        private const val EXTRA_VIDEO_ID = "video_id"
        private const val EXTRA_TITLE = "title"

        fun intent(ctx: Context, siteKey: String, videoId: String, title: String) =
            Intent(ctx, DetailActivity::class.java).apply {
                putExtra(EXTRA_SITE_KEY, siteKey)
                putExtra(EXTRA_VIDEO_ID, videoId)
                putExtra(EXTRA_TITLE, title)
            }
    }
}
