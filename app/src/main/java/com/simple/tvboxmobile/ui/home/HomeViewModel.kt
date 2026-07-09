package com.simple.tvboxmobile.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simple.tvbox.model.Source
import com.simple.tvbox.source.DoubanService
import com.simple.tvboxmobile.data.SourceAccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    data class HomeUiState(
        val isLoading: Boolean = false,
        val sources: List<Source> = emptyList(),
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
            _state.value = _state.value.copy(isLoading = false, sources = list)
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
