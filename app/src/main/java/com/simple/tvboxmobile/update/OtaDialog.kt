package com.simple.tvboxmobile.update

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment

@Composable
fun OtaDialog(vm: OtaViewModel) {
    val state by vm.state.collectAsState()

    // Error snackbar
    if (state.error != null) {
        AlertDialog(
            onDismissRequest = { vm.dismissError() },
            title = { Text("出错") },
            text = { Text(state.error!!) },
            confirmButton = { TextButton(onClick = { vm.dismissError() }) { Text("关闭") } }
        )
    }

    // Need install permission
    if (state.needInstallPermission) {
        AlertDialog(
            onDismissRequest = { vm.dismiss() },
            title = { Text("需要授权") },
            text = { Text("OTA 升级需要先授权「安装未知来源应用」，点击前往设置。") },
            confirmButton = {
                TextButton(onClick = {
                    vm.openInstallPermissionSettings()
                    vm.dismiss()
                }) { Text("前往设置") }
            },
            dismissButton = { TextButton(onClick = { vm.dismiss() }) { Text("取消") } }
        )
    }

    // No update
    if (state.noUpdate) {
        AlertDialog(
            onDismissRequest = { vm.dismiss() },
            title = { Text("已是最新版本") },
            text = { Text("当前版本已经是最新了") },
            confirmButton = { TextButton(onClick = { vm.dismiss() }) { Text("好的") } }
        )
    }

    // Has update
    if (state.info != null && !state.downloading && !state.downloadComplete) {
        val info = state.info!!
        AlertDialog(
            onDismissRequest = { vm.dismiss() },
            title = { Text("发现新版本 v${info.versionName}") },
            text = {
                Column {
                    info.changelog.take(8).forEach { line ->
                        Text("• $line", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(2.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("APK 大小: ${formatSize(info.apkSize)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = { TextButton(onClick = { vm.startDownload() }) { Text("立即更新") } },
            dismissButton = { TextButton(onClick = { vm.dismiss() }) { Text("稍后再说") } }
        )
    }

    // Downloading
    if (state.downloading) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("正在下载 v${state.info?.versionName ?: ""}") },
            text = {
                Column {
                    LinearProgressIndicator(
                        progress = { state.progress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("${state.progress}%  (${formatSize(state.downloadedBytes)} / ${formatSize(state.totalBytes)})",
                        style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {}
        )
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "未知"
    val mb = bytes / 1024.0 / 1024.0
    return if (mb >= 1.0) "%.1f MB".format(mb) else "${bytes / 1024} KB"
}
