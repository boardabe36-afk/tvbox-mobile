package com.simple.tvboxmobile.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simple.tvbox.source.VideoClientFactory
import com.simple.tvboxmobile.data.SourceAccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DetailViewModel : ViewModel() {
    val loading = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)
    val episodes = MutableStateFlow<List<Pair<String, String>>>(emptyList())

    fun load(siteKey: String, videoId: String) {
        loading.value = true
        error.value = null
        viewModelScope.launch {
            try {
                val site = withContext(Dispatchers.IO) {
                    SourceAccess.repository().loadAllSites().find { it.key == siteKey }
                }
                if (site == null) { error.value = "找不到视频源"; loading.value = false; return@launch }
                val client = VideoClientFactory.create(site)
                if (!client.isSupported()) { error.value = "不支持的源类型"; loading.value = false; return@launch }
                val eps = withContext(Dispatchers.IO) { client.fetchEpisodes(videoId) }
                episodes.value = eps
            } catch (t: Throwable) {
                error.value = t.message ?: "未知错误"
            } finally {
                loading.value = false
            }
        }
    }
}
