package com.simple.tvboxmobile.ui.player

import android.content.pm.ActivityInfo
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.simple.tvbox.model.WatchHistoryItem
import com.simple.tvbox.source.VideoClientFactory
import com.simple.tvboxmobile.data.SourceAccess
import com.simple.tvboxmobile.data.WatchHistoryStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun PlayerScreen(
    videoId: String,
    siteKey: String,
    title: String,
    episodeUrl: String,
    onBack: () -> Unit
) {
    var playUrl by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var isLandscape by remember { mutableStateOf(false) }

    val historyKey = "$siteKey:$videoId:$episodeUrl"
    val saved = remember { WatchHistoryStore.find(historyKey) }
    val context = LocalContext.current
    val activity = remember { context.findActivity() }

    // Apply orientation
    LaunchedEffect(isLandscape) {
        activity?.requestedOrientation = if (isLandscape)
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        else
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    // Reset orientation on exit
    BackHandler {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        onBack()
    }

    LaunchedEffect(videoId, siteKey, episodeUrl) {
        loading = true
        error = null
        playUrl = null
        try {
            val site = withContext(Dispatchers.IO) {
                SourceAccess.repository().loadAllSites().find { it.key == siteKey }
            }
            if (site == null) { error = "找不到视频源"; loading = false; return@LaunchedEffect }
            val client = VideoClientFactory.create(site)
            if (!client.isSupported()) { error = "不支持的源类型"; loading = false; return@LaunchedEffect }

            val resolved = withContext(Dispatchers.IO) {
                client.resolvePlayUrl(episodeUrl).url
            }
            if (resolved.isBlank()) { error = "无法解析播放地址" }
            else { playUrl = resolved }
        } catch (t: Throwable) {
            error = t.message ?: "未知错误"
        } finally {
            loading = false
        }
    }

    // Save position periodically
    LaunchedEffect(playUrl, exoPlayer) {
        while (playUrl != null && exoPlayer != null) {
            delay(5000)
            exoPlayer?.let { p ->
                val pos = p.currentPosition
                val dur = p.duration
                if (dur > 0) {
                    WatchHistoryStore.save(WatchHistoryItem(
                        key = historyKey, title = title, siteKey = siteKey,
                        videoId = videoId, episodeUrl = episodeUrl,
                        resolvedUrl = playUrl, positionMs = pos, durationMs = dur,
                        updatedAt = System.currentTimeMillis()
                    ))
                }
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text("正在解析播放地址...")
                }
            }
            error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("播放失败: $error", color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = {
                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                        onBack()
                    }) { Text("返回") }
                }
            }
            playUrl != null -> {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        val mediaItem = androidx.media3.common.MediaItem.Builder()
                            .setUri(playUrl!!)
                            .build()
                        PlayerView(ctx).apply {
                            val ep = ExoPlayer.Builder(ctx).build().apply {
                                setMediaItem(mediaItem)
                                prepare()
                                playWhenReady = true
                                saved?.let { h ->
                                    if (h.positionMs > 0 && h.positionMs < (h.durationMs - 5000)) {
                                        seekTo(h.positionMs)
                                    }
                                }
                                addListener(object : Player.Listener {
                                    override fun onPlaybackStateChanged(state: Int) {
                                        if (state == Player.STATE_ENDED) {
                                            WatchHistoryStore.save(WatchHistoryItem(
                                                key = historyKey, title = title, siteKey = siteKey,
                                                videoId = videoId, episodeUrl = episodeUrl,
                                                resolvedUrl = playUrl, positionMs = 0,
                                                durationMs = duration, updatedAt = System.currentTimeMillis()
                                            ))
                                        }
                                    }
                                })
                            }
                            exoPlayer = ep
                            player = ep
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    }
                )
                // Landscape toggle button (top-right)
                FloatingActionButton(
                    onClick = { isLandscape = !isLandscape },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                ) {
                    Icon(Icons.Default.ScreenRotation, contentDescription = "横屏切换",
                        tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

// Helper to find Activity from Context
private fun android.content.Context.findActivity(): android.app.Activity? {
    var ctx = this
    while (ctx is android.content.ContextWrapper) {
        if (ctx is android.app.Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
