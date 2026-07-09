package com.simple.tvboxmobile.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    siteKey: String,
    videoId: String,
    title: String,
    onPlayEpisode: (episodeUrl: String, title: String) -> Unit,
    onBack: () -> Unit,
    vm: DetailViewModel = viewModel()
) {
    // CRITICAL: must use collectAsState() not .value, otherwise Compose won't recompose
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val episodes by vm.episodes.collectAsState()

    LaunchedEffect(siteKey, videoId) { vm.load(siteKey, videoId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            when {
                loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                error != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("加载失败: $error", color = MaterialTheme.colorScheme.error)
                    }
                }
                episodes.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("没有可播放的剧集")
                    }
                }
                else -> {
                    Text(
                        "共 ${episodes.size} 集",
                        Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleSmall
                    )
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(episodes) { (name, url) ->
                            EpisodeChip(
                                name = name,
                                onClick = { onPlayEpisode(url, title) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EpisodeChip(name: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null,
                modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(4.dp))
            Text(name, fontSize = 13.sp, maxLines = 1, fontWeight = FontWeight.Medium)
        }
    }
}
