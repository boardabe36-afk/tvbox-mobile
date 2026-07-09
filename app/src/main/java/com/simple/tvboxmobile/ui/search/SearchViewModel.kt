package com.simple.tvboxmobile.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simple.tvbox.model.SpiderSite
import com.simple.tvbox.model.VideoItem
import com.simple.tvbox.source.VideoClientFactory
import com.simple.tvbox.util.MatchScorer
import com.simple.tvboxmobile.data.SourceAccess
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {

    data class SearchResult(
        val site: SpiderSite,
        val item: VideoItem,
        val score: Int
    )

    data class State(
        val query: String = "",
        val isSearching: Boolean = false,
        val results: List<SearchResult> = emptyList()
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(q: String) {
        _state.value = _state.value.copy(query = q)
        search()
    }

    /**
     * 复用 tvbox-simple v1.0.13 的搜索逻辑：
     * - 全源并发搜索（async / Dispatchers.IO）
     * - 每个源返回的结果用 MatchScorer 打分
     * - 过滤阈值 > 500 + 按分数降序
     */
    private fun search() {
        val q = _state.value.query.trim()
        if (q.isEmpty()) {
            _state.value = _state.value.copy(isSearching = false, results = emptyList())
            return
        }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _state.value = _state.value.copy(isSearching = true)
            // 防抖：300ms 内连续输入不重复请求
            delay(300)
            try {
                val sites = SourceAccess.repository().loadAllSites()
                if (sites.isEmpty()) {
                    _state.value = _state.value.copy(isSearching = false, results = emptyList())
                    return@launch
                }
                val collected: List<SearchResult> = coroutineScope {
                    sites.map { site ->
                        async(kotlinx.coroutines.Dispatchers.IO) {
                            val client = VideoClientFactory.create(site)
                            if (!client.isSupported()) return@async emptyList<SearchResult>()
                            val items: List<VideoItem> = runCatching { client.search(q, 1) }
                                .getOrDefault(emptyList())
                            items.map { it to MatchScorer.score(q, it.title, it.subTitle) }
                                .filter { it.second > 500 }
                                .map { (item, score) -> SearchResult(site, item, score) }
                        }
                    }.awaitAll().flatten()
                }
                val ranked = collected
                    .groupBy { "${it.site.key}::${it.item.id}" }
                    .map { (_, g) -> g.maxByOrNull { it.score }!! }
                    .sortedWith(
                        compareByDescending<SearchResult> { it.score }
                            .thenBy { it.item.title.length }
                    )
                _state.value = State(query = q, isSearching = false, results = ranked)
            } catch (t: Throwable) {
                android.util.Log.e("Search", "search failed", t)
                _state.value = _state.value.copy(isSearching = false, results = emptyList())
            }
        }
    }
}
