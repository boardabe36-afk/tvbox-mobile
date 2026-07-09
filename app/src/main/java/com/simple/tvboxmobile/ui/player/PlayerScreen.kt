package com.simple.tvboxmobile.ui.player

import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.simple.tvbox.model.SpiderSite
import com.simple.tvbox.source.VideoClientFactory
import com.simple.tvboxmobile.data.SourceAccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PlayerScreen(
    videoId: String,
    siteKey: String,
    title: String,
    onBack: () -> Unit
) {
    var playUrl by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(videoId, siteKey) {
        loading = true
        error = null
        playUrl = null
        try {
            val repo = SourceAccess.repository()
            // 从已加载的站点列表中找到对应的 site
            val sites = repo.loadAllSites()
            val site = sites.find { it.key == siteKey }
            if (site == null) {
                error = "找不到对应的视频源"
                loading = false
                return@LaunchedEffect
            }

            val client = VideoClientFactory.create(site)
            if (!client.isSupported()) {
                error = "暂不支持的源类型"
                loading = false
                return@LaunchedEffect
            }

            val url = withContext(Dispatchers.IO) {
                val info = client.fetchDetailInfo(videoId)
                info?.optString("playUrl", "")
            }
            if (url.isNullOrBlank()) {
                error = "无法解析播放地址"
            } else {
                playUrl = url
            }
        } catch (t: Throwable) {
            error = t.message ?: "未知错误"
        } finally {
            loading = false
        }
    }

    BackHandler { onBack() }

    when {
        loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text("正在解析播放地址...")
                }
            }
        }
        error != null -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("播放失败: $error", color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onBack) { Text("返回") }
                }
            }
        }
        playUrl != null -> {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = ExoPlayer.Builder(ctx).build().apply {
                            setMediaItem(MediaItem.fromUri(playUrl!!))
                            prepare()
                            playWhenReady = true
                        }
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                }
            )
        }
    }
}
