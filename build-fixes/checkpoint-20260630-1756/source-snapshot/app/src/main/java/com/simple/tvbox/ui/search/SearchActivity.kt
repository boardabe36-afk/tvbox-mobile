package com.simple.tvbox.ui.search

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.simple.tvbox.R
import com.simple.tvbox.TvBoxApp
import com.simple.tvbox.model.SpiderSite
import com.simple.tvbox.model.VideoItem
import com.simple.tvbox.source.VideoClientFactory
import com.simple.tvbox.ui.detail.DetailActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 搜索页：聚合多个源做搜索，结果按源分组展示。
 *
 * 简版：用 ScrollView + 嵌套 LinearLayout 渲染"源分组 + 卡片网格"，简单稳。
 * - 不依赖 VerticalGridSupportFragment（那个独立用到 FragmentActivity 上会崩）
 * - 卡片支持触屏点击 + 遥控器 OK 键
 */
class SearchActivity : FragmentActivity() {

    private lateinit var searchInput: EditText
    private lateinit var progress: ProgressBar
    private lateinit var emptyText: TextView
    private lateinit var resultsContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        searchInput = findViewById(R.id.search_input)
        progress = findViewById(R.id.search_progress)
        emptyText = findViewById(R.id.search_empty)
        resultsContainer = findViewById(R.id.search_results_container)

        searchInput.setOnEditorActionListener { _, actionId, event ->
            val isSubmit = actionId == EditorInfo.IME_ACTION_SEARCH ||
                actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER &&
                    event.action == KeyEvent.ACTION_DOWN)
            if (isSubmit) {
                doSearch()
                true
            } else {
                false
            }
        }
        searchInput.requestFocus()

        intent.getStringExtra(EXTRA_QUERY)?.takeIf { it.isNotBlank() }?.let { query ->
            searchInput.setText(query)
            searchInput.setSelection(query.length)
            doSearch()
        }
    }

    private fun doSearch() {
        val keyword = searchInput.text.toString().trim()
        if (keyword.isBlank()) return
        val sources = TvBoxApp.get().sourceRepository.getAllSources()
        if (sources.isEmpty()) {
            Toast.makeText(this, R.string.search_need_source, Toast.LENGTH_LONG).show()
            return
        }
        progress.visibility = View.VISIBLE
        emptyText.visibility = View.GONE
        resultsContainer.removeAllViews()

        lifecycleScope.launch {
            try {
                val sites: List<SpiderSite> = withContext(Dispatchers.IO) {
                    TvBoxApp.get().sourceRepository.loadAllSites()
                }

                if (sites.isEmpty()) {
                    progress.visibility = View.GONE
                    emptyText.visibility = View.VISIBLE
                    emptyText.text = "没有兼容的视频源（请在设置中检查源）"
                    return@launch
                }

                // 并发搜索所有站点
                val results: List<Pair<SpiderSite, List<VideoItem>>> = withContext(Dispatchers.IO) {
                    sites.map { site ->
                        async {
                            runCatching {
                                val client = VideoClientFactory.create(site)
                                if (client.isSupported()) {
                                    site to client.search(keyword, 1)
                                } else null
                            }.getOrNull()
                        }
                    }.awaitAll().filterNotNull()
                }

                progress.visibility = View.GONE
                val flat = results.sumOf { it.second.size }
                if (flat == 0) {
                    emptyText.visibility = View.VISIBLE
                    emptyText.text = getString(R.string.search_no_results)
                } else {
                    renderResults(results)
                }
            } catch (t: Throwable) {
                progress.visibility = View.GONE
                Toast.makeText(this@SearchActivity, "搜索失败：${t.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * 渲染搜索结果：每个源一个分组（标题 + 横向滚动的卡片列表）。
     * 用 HorizontalScrollView + LinearLayout 实现"行内多个卡片"。
     */
    private fun renderResults(results: List<Pair<SpiderSite, List<VideoItem>>>) {
        resultsContainer.removeAllViews()
        results.forEach { (site, items) ->
            if (items.isEmpty()) return@forEach
            // 分组标题
            val titleView = TextView(this).apply {
                text = "${site.name}  ·  ${items.size} 条结果"
                textSize = 20f
                setTextColor(Color.WHITE)
                setPadding(0, 24, 0, 16)
            }
            resultsContainer.addView(titleView)

            // 横向滚动卡片
            val scroll = HorizontalScrollView(this).apply {
                isFocusable = false
                scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
            }
            val cardRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }
            items.take(50).forEach { v ->
                cardRow.addView(buildCardView(v))
            }
            scroll.addView(cardRow)
            resultsContainer.addView(scroll)
        }
    }

    private fun buildCardView(v: VideoItem): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_card)
            isFocusable = true
            isFocusableInTouchMode = true
            isClickable = true
            val size = dpToPx(180)
            layoutParams = LinearLayout.LayoutParams(size, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                marginEnd = dpToPx(12)
                bottomMargin = dpToPx(8)
            }
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
            setOnClickListener {
                startActivity(
                    DetailActivity.intent(
                        this@SearchActivity,
                        siteKey = v.sourceKey,
                        videoId = v.id,
                        title = v.title
                    )
                )
            }
        }
        card.addView(TextView(this).apply {
            text = v.title
            textSize = 16f
            setTextColor(Color.WHITE)
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
        })
        v.subTitle?.takeIf { it.isNotBlank() }?.let { sub ->
            card.addView(TextView(this).apply {
                text = sub
                textSize = 12f
                setTextColor(Color.LTGRAY)
                maxLines = 1
                setPadding(0, dpToPx(6), 0, 0)
            })
        }
        return card
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()

    companion object {
        private const val EXTRA_QUERY = "query"

        fun intent(ctx: android.content.Context, query: String? = null) =
            Intent(ctx, SearchActivity::class.java).apply {
                if (!query.isNullOrBlank()) putExtra(EXTRA_QUERY, query)
            }
    }
}
