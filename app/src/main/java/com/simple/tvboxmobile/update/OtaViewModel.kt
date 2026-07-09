package com.simple.tvboxmobile.update

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OtaState(
    val checking: Boolean = false,
    val info: OtaService.UpdateInfo? = null,
    val downloading: Boolean = false,
    val progress: Int = 0,
    val downloadedBytes: Long = 0,
    val totalBytes: Long = 0,
    val error: String? = null,
    val noUpdate: Boolean = false,
    val needInstallPermission: Boolean = false,
    val downloadComplete: Boolean = false
)

class OtaViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow(OtaState())
    val state: StateFlow<OtaState> = _state.asStateFlow()

    fun checkForUpdate() {
        if (_state.value.checking) return
        _state.value = OtaState(checking = true)
        viewModelScope.launch {
            try {
                val info = OtaService.fetchUpdateInfo()
                val currentCode = getApplication<Application>().packageManager
                    .getPackageInfo(getApplication<Application>().packageName, 0).versionCode
                if (info.versionCode > currentCode) {
                    _state.value = _state.value.copy(checking = false, info = info)
                } else {
                    _state.value = _state.value.copy(checking = false, noUpdate = true)
                }
            } catch (t: Throwable) {
                _state.value = _state.value.copy(checking = false, error = t.message ?: t.javaClass.simpleName)
            }
        }
    }

    fun startDownload() {
        val info = _state.value.info ?: return
        val ctx = getApplication<Application>()
        if (!OtaService.canInstallUnknownApks(ctx)) {
            _state.value = _state.value.copy(needInstallPermission = true)
            return
        }
        _state.value = _state.value.copy(downloading = true, progress = 0, error = null)
        viewModelScope.launch {
            try {
                val file = OtaService.downloadApk(ctx, info) { percent, downloaded, total ->
                    _state.value = _state.value.copy(progress = percent, downloadedBytes = downloaded, totalBytes = total)
                }
                _state.value = _state.value.copy(downloading = false, downloadComplete = true)
                OtaService.installApk(ctx, file)
            } catch (t: Throwable) {
                _state.value = _state.value.copy(downloading = false, error = t.message ?: t.javaClass.simpleName)
            }
        }
    }

    fun openInstallPermissionSettings() {
        val ctx = getApplication<Application>()
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = Uri.parse("package:${ctx.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { ctx.startActivity(intent) }
    }

    fun dismiss() {
        _state.value = OtaState()
    }

    fun dismissError() {
        _state.value = _state.value.copy(error = null)
    }
}
