package com.simple.tvboxmobile.ui.player

import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.simple.tvbox.source.VideoClientFactory
import com.simple.tvboxmobile.data.SourceAccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ExoPlayer 播放页（v1 MVP：单视频，无选集）。
 *
 * 流程：
 *  1. 用 siteKey 找到对应的 SpiderSite
 *  2. 用 VideoClientFactory 创建 client
 *  3. client.fetchDetailInfo(videoId) -> PlayInfo.url
 *  4. ExoPlayer 播放
 */
@Composable
fun PlayerScreen(
    videoId: String,
    siteKey: String,
    title: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var playUrl by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(videoId, siteKey) {
        try {
            val repo = SourceAccess.repository()
            // 找 site：SpiderSite.key == siteKey，但需要 sourceUrl 信息才能 locate
            // 简单实现：直接用 siteKey 构造 SpiderSite 假设是 HTML 源
            val fakeSite = com.simple.tvbox.model.SpiderSite(
                key = siteKey,
                name = title,
                type = 1,
                api = "html://$videoId"  // 临时 MVP 通道
            )
            val client = VideoClientFactory.create(fakeSite)
            if (!client.isSupported()) {
                error = "暂不支持的源类型"
                return@LaunchedEffect
            }
            val playInfo = withContext(Dispatchers.IO) {
                val info = client.fetchDetailInfo(videoId)
                info?.let {
                    val url = it.optString("playUrl", "")
                    if (url.isNotBlank()) url else null
                }
            }
            if (playInfo == null) {
                error = "无法解析播放地址"
            } else {
                playUrl = playInfo
            }
        } catch (t: Throwable) {
            error = t.message ?: "未知错误"
        }
    }

    BackHandler { onBack() }

    when {
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
        error != null -> {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                androidx.compose.material3.Text("播放失败: $error")
            }
        }
        else -> {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                androidx.compose.material3.CircularProgressIndicator()
            }
        }
    }
}
