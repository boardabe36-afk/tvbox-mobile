package com.simple.tvboxmobile.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simple.tvbox.model.Source
import com.simple.tvboxmobile.data.SourceAccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    data class HomeUiState(
        val isLoading: Boolean = false,
        val sources: List<Source> = emptyList()
    )

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _state.value = _state.value.copy(isLoading = true)
        viewModelScope.launch {
            val list = SourceAccess.all()
            _state.value = HomeUiState(isLoading = false, sources = list)
        }
    }
}
