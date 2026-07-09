package com.simple.tvboxmobile.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simple.tvbox.model.WatchHistoryItem
import com.simple.tvboxmobile.data.WatchHistoryStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onResume: (WatchHistoryItem) -> Unit
) {
    val items = remember { mutableStateListOf<WatchHistoryItem>() }

    LaunchedEffect(Unit) { items.clear(); items.addAll(WatchHistoryStore.all()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("观看历史") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (items.isNotEmpty()) {
                        TextButton(onClick = {
                            WatchHistoryStore.clear()
                            items.clear()
                        }) { Text("清空") }
                    }
                }
            )
        }
    ) { padding ->
        if (items.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("还没有观看记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items, key = { it.key }) { item ->
                    HistoryRow(item,
                        onClick = { onResume(item) },
                        onDelete = {
                            WatchHistoryStore.remove(item.key)
                            items.removeAll { it.key == item.key }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(item: WatchHistoryItem, onClick: () -> Unit, onDelete: () -> Unit) {
    Surface(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(item.title, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                val pct = if (item.durationMs > 0) (item.positionMs * 100 / item.durationMs) else 0
                Text("已观看 $pct%", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.PlayArrow, contentDescription = "继续", tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
