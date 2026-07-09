package com.simple.tvboxmobile.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simple.tvbox.model.Source
import com.simple.tvbox.model.VideoCategory
import com.simple.tvbox.source.DoubanService
import com.simple.tvbox.source.VideoClientFactory
import com.simple.tvboxmobile.data.SourceAccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel : ViewModel() {

    data class SourceCategory(
        val siteName: String,
        val siteKey: String,
        val categories: List<VideoCategory>
    )

    data class HomeUiState(
        val isLoading: Boolean = false,
        val sources: List<Source> = emptyList(),
        val sourceCategories: List<SourceCategory> = emptyList(),
        val doubanMovies: List<DoubanService.DoubanItem> = emptyList(),
        val doubanTvs: List<DoubanService.DoubanItem> = emptyList()
    )

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        refresh()
        loadDouban()
        viewModelScope.launch {
            SourceAccess.version.collect { refresh() }
        }
    }

    fun refresh() {
        _state.value = _state.value.copy(isLoading = true)
        viewModelScope.launch {
            val list = SourceAccess.all()
            val sites = withContext(Dispatchers.IO) {
                SourceAccess.repository().loadAllSites()
            }
            val cats = mutableListOf<SourceCategory>()
            for (site in sites) {
                try {
                    val client = VideoClientFactory.create(site)
                    if (client.isSupported()) {
                        val homeCats = withContext(Dispatchers.IO) {
                            client.fetchHomeCategories()
                        }
                        if (homeCats.isNotEmpty()) {
                            cats.add(SourceCategory(site.name, site.key, homeCats))
                        }
                    }
                } catch (_: Throwable) {}
            }
            _state.value = _state.value.copy(isLoading = false, sources = list, sourceCategories = cats)
        }
    }

    private fun loadDouban() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val movies = DoubanService.fetchChannel("movie_hot")
                val tvs = DoubanService.fetchChannel("tv_hot")
                _state.value = _state.value.copy(doubanMovies = movies, doubanTvs = tvs)
            } catch (t: Throwable) {
                android.util.Log.e("Home", "douban failed", t)
            }
        }
    }
}
