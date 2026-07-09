package com.simple.tvbox.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.lifecycle.lifecycleScope
import com.simple.tvbox.R
import com.simple.tvbox.TvBoxApp
import com.simple.tvbox.model.Source
import com.simple.tvbox.model.SpiderSite
import com.simple.tvbox.model.VideoCategory
import com.simple.tvbox.source.VideoClientFactory
import com.simple.tvbox.ui.search.SearchActivity
import com.simple.tvbox.ui.settings.SettingsActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 主入口。
 *
 * 行结构：
 *   1. 快捷入口行：搜索 / 设置（固定存在）
 *   2. 已配置源行：用户添加的所有源（点击查看该源分类）
 *   3. 分类快捷行：每个已加载站点的分类（点击进入分类页）
 *   4. 空状态行：未配置源时引导用户去设置
 *
 * 关键：onResume() 重新加载，避免从设置页添加源后回到首页看不到
 */
class HomeFragment : BrowseSupportFragment() {

    private val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.app_name)
        headersState = HEADERS_ENABLED
        isHeadersTransitionOnBackEnabled = true
        brandColor = ContextCompat.getColor(requireContext(), R.color.tv_primary)
        searchAffordanceColor = ContextCompat.getColor(requireContext(), R.color.tv_accent)

        // 搜索图标点击
        setOnSearchClickedListener {
            startActivity(Intent(requireContext(), SearchActivity::class.java))
        }
        // 行内点击
        onItemViewClickedListener = OnItemViewClickedListener { _, item, _, _ ->
            handleItemClick(item)
        }

        adapter = rowsAdapter
    }

    override fun onResume() {
        super.onResume()
        // 每次回到主页都重载（清空再重新渲染），保证新加的源能立即出现
        loadHome()
    }

    private fun handleItemClick(item: Any?) {
        when (item) {
            is VideoCard -> {
                val intent = if (item.categoryId != null) {
                    // 分类卡片：直接进分类视频列表
                    CategoryActivity.intent(
                        requireContext(),
                        siteKey = item.siteKey,
                        categoryId = item.categoryId,
                        categoryName = item.title
                    )
                } else {
                    // 源卡片：跳到源详情（传源 URL）
                    SourceDetailActivity.intent(
                        requireContext(),
                        siteKey = item.siteKey,
                        sourceName = item.title,
                        sourceUrl = item.id   // VideoCard.id 在 renderConfiguredSourcesRow 里存的就是 src.url
                    )
                }
                startActivity(intent)
            }
            is ActionItem -> item.onClick()
        }
    }

    private fun loadHome() {
        rowsAdapter.clear()
        lifecycleScope.launch {
            try {
                val sources = TvBoxApp.get().sourceRepository.getAllSources()
                if (sources.isEmpty()) {
                    renderEmptyState()
                    return@launch
                }
                // 已有源：渲染丰富的首页
                renderQuickEntryRow()
                renderConfiguredSourcesRow(sources)
                renderSiteCategoryRows(sources)
            } catch (t: Throwable) {
                Toast.makeText(
                    requireContext(),
                    "加载源失败：${t.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun renderEmptyState() {
        val header = HeaderItem(0, getString(R.string.title_sources))
        val item = ActionItem(
            id = 1L,
            title = getString(R.string.title_settings),
            subTitle = getString(R.string.settings_empty)
        ) {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }
        val rowAdapter = ArrayObjectAdapter(ActionPresenter())
        rowAdapter.add(item)
        rowsAdapter.add(ListRow(header, rowAdapter))
    }

    /**
     * 行 1：快捷入口（搜索 / 设置）
     */
    private fun renderQuickEntryRow() {
        val header = HeaderItem(HEADER_QUICK, getString(R.string.title_quick))
        val rowAdapter = ArrayObjectAdapter(ActionPresenter())
        rowAdapter.add(ActionItem(id = HEADER_QUICK * 100 + 1, title = getString(R.string.title_search)) {
            startActivity(Intent(requireContext(), SearchActivity::class.java))
        })
        rowAdapter.add(ActionItem(id = HEADER_QUICK * 100 + 2, title = getString(R.string.title_settings)) {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        })
        rowAdapter.add(ActionItem(id = HEADER_QUICK * 100 + 3, title = getString(R.string.title_refresh)) {
            // 手动刷新：清空所有缓存后重新加载
            TvBoxApp.get().sourceRepository.invalidateCache()
            Toast.makeText(requireContext(), R.string.refreshing, Toast.LENGTH_SHORT).show()
            loadHome()
        })
        rowsAdapter.add(ListRow(header, rowAdapter))
    }

    /**
     * 行 2：已配置的源（点击查看该源分类页）
     */
    private fun renderConfiguredSourcesRow(sources: List<Source>) {
        val header = HeaderItem(HEADER_SOURCES, getString(R.string.title_sources) + "（${sources.size}）")
        val rowAdapter = ArrayObjectAdapter(CardPresenter())
        sources.forEach { src ->
            val isHtml = src.kind == Source.Kind.HTML
            rowAdapter.add(VideoCard(
                id = src.url,
                title = src.name,
                subTitle = if (isHtml) "HTML 网站" else "JSON 协议",
                poster = null,
                siteKey = if (isHtml) TvBoxApp.get().sourceRepository.htmlSiteKey(src.url) else src.url,
                categoryId = null
            ))
        }
        rowsAdapter.add(ListRow(header, rowAdapter))
    }

    /**
     * 行 3+：每个站点的分类（异步加载）
     */
    private fun renderSiteCategoryRows(sources: List<Source>) {
        sources.forEachIndexed { idx, src ->
            val isHtml = src.kind == Source.Kind.HTML
            val siteKey = if (isHtml) TvBoxApp.get().sourceRepository.htmlSiteKey(src.url) else src.url
            val siteKind = if (isHtml) "html://${src.url}" else src.url

            // 对 HTML 源：站点就是它自己（无需展开），直接探测一次
            // 对 JSON 源：需要展开成 SpiderSite 再 fetchHomeCategories
            lifecycleScope.launch {
                runCatching {
                    val site: SpiderSite? = if (isHtml) {
                        // HTML 源站点：包装为 SpiderSite
                        withContext(Dispatchers.IO) {
                            TvBoxApp.get().sourceRepository.testAndLoad(src)
                        }
                    } else {
                        null // JSON 源等下面异步处理
                    }
                    if (site != null) {
                        addCategoryRowForSite(
                            rowIndex = HEADER_SITES + idx,
                            headerTitle = site.name,
                            siteKey = siteKey,
                            api = siteKind
                        )
                    } else if (!isHtml) {
                        // JSON 源：尝试加载第一个站点
                        val allSites = withContext(Dispatchers.IO) {
                            TvBoxApp.get().sourceRepository.loadAllSites()
                        }
                        allSites.firstOrNull { it.key == siteKey }?.let { s ->
                            addCategoryRowForSite(
                                rowIndex = HEADER_SITES + idx,
                                headerTitle = s.name,
                                siteKey = s.key,
                                api = s.api
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * 添加某个站点的分类行（异步拉分类）
     */
    private fun addCategoryRowForSite(rowIndex: Long, headerTitle: String, siteKey: String, api: String) {
        val client = VideoClientFactory.create(
            SpiderSite(
                key = siteKey,
                name = headerTitle,
                type = 1,
                api = api
            )
        )
        if (!client.isSupported()) return

        val rowAdapter = ArrayObjectAdapter(CardPresenter())
        // 占位
        rowAdapter.add(VideoCard(
            id = "${siteKey}__loading",
            title = headerTitle,
            subTitle = getString(R.string.loading),
            poster = null,
            siteKey = siteKey,
            categoryId = null
        ))
        val row = ListRow(HeaderItem(rowIndex, headerTitle), rowAdapter)
        rowsAdapter.add(row)

        lifecycleScope.launch {
            runCatching {
                val cats: List<VideoCategory> = withContext(Dispatchers.IO) {
                    client.fetchHomeCategories()
                }
                // 注意：rowsAdapter 可能已经被 loadHome 重建了，所以这里要检查 row 是否还在
                if (rowsAdapter.indexOf(row) < 0) return@runCatching
                rowAdapter.clear()
                if (cats.isEmpty()) {
                    rowAdapter.add(VideoCard(
                        id = "${siteKey}__empty",
                        title = headerTitle,
                        subTitle = "（无分类）",
                        poster = null,
                        siteKey = siteKey
                    ))
                } else {
                    cats.take(20).forEach { cat ->
                        rowAdapter.add(VideoCard(
                            id = "${siteKey}__${cat.id}",
                            title = cat.name,
                            subTitle = headerTitle,
                            poster = null,
                            siteKey = siteKey,
                            categoryId = cat.id
                        ))
                    }
                }
            }
        }
    }

    companion object {
        private const val HEADER_QUICK = 0L
        private const val HEADER_SOURCES = 1L
        private const val HEADER_SITES = 100L
    }
}

/** 顶部按钮 / 快捷操作 */
data class ActionItem(
    val id: Long,
    val title: String,
    val subTitle: String? = null,
    val onClick: () -> Unit
)

/** 视频卡片 */
data class VideoCard(
    val id: String,
    val title: String,
    val subTitle: String?,
    val poster: String?,
    val siteKey: String,
    val categoryId: String? = null
)
