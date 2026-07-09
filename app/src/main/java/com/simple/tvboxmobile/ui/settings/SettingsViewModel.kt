package com.simple.tvboxmobile.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simple.tvbox.model.Source
import com.simple.tvboxmobile.data.SourceAccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {

    data class State(
        val sources: List<Source> = emptyList()
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init { reload() }

    fun reload() {
        viewModelScope.launch {
            _state.value = State(sources = SourceAccess.all())
        }
    }

    fun addSource(name: String, url: String, kind: Source.Kind) {
        viewModelScope.launch {
            try {
                SourceAccess.add(Source(name = name, url = url, kind = kind))
                reload()
            } catch (t: Throwable) {
                android.util.Log.e("Settings", "addSource failed", t)
            }
        }
    }

    fun remove(src: Source) {
        viewModelScope.launch {
            SourceAccess.remove(src)
            reload()
        }
    }
}
