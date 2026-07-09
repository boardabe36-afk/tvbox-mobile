package com.simple.tvboxmobile.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.simple.tvbox.model.Source
import com.simple.tvbox.source.DoubanService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSettingsClick: () -> Unit,
    onSearchClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onDoubanClick: (String) -> Unit,
    onCategoryClick: (String, String, String) -> Unit,
    vm: HomeViewModel = viewModel()
) {
    val state by vm.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TVBox", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = onHistoryClick) {
                        Icon(Icons.Default.History, contentDescription = "History")
                    }
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onSearchClick) {
                Icon(Icons.Default.Search, contentDescription = "Search")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item { BannerCard(sourcesCount = state.sources.size) }

            // Source categories
            if (state.sourceCategories.isNotEmpty()) {
                item {
                    SectionHeader("Categories")
                }
                state.sourceCategories.forEach { sc ->
                    item {
                        Text(
                            sc.siteName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                    item {
                        CategoryChips(
                            categories = sc.categories,
                            onClick = { cat ->
                                onCategoryClick(sc.siteKey, cat.id, cat.name)
                            }
                        )
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }

            // Douban movies
            if (state.doubanMovies.isNotEmpty()) {
                item {
                    SectionHeader("Hot Movies")
                    DoubanRow(items = state.doubanMovies, onClick = { onDoubanClick(it) })
                }
            }

            // Douban TV
            if (state.doubanTvs.isNotEmpty()) {
                item {
                    SectionHeader("Hot TV Series")
                    DoubanRow(items = state.doubanTvs, onClick = { onDoubanClick(it) })
                }
            }

            // Sources list
            item {
                SectionHeader("Sources (${state.sources.size})", onClick = onSettingsClick)
            }

            if (state.isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (state.sources.isEmpty()) {
                item { EmptySourceHint(onAdd = onSettingsClick) }
            } else {
                items(state.sources) { src -> SourceRow(source = src) }
            }
        }
    }
}

@Composable
private fun BannerCard(sourcesCount: Int) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("TVBox Mobile", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary)
            Text(
                if (sourcesCount == 0) "Go to Settings to add your first video source"
                else "Configured $sourcesCount video sources. Tap search to start watching.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, onClick: () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        TextButton(onClick = onClick) { Text("More >") }
    }
}

@Composable
private fun CategoryChips(
    categories: List<com.simple.tvbox.model.VideoCategory>,
    onClick: (com.simple.tvbox.model.VideoCategory) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { cat ->
            AssistChip(
                onClick = { onClick(cat) },
                label = { Text(cat.name) },
                leadingIcon = { Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
        }
    }
}

@Composable
private fun DoubanRow(items: List<DoubanService.DoubanItem>, onClick: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items.take(10)) { item ->
            DoubanCard(item, onClick = { onClick(item.title) })
        }
    }
}

@Composable
private fun DoubanCard(item: DoubanService.DoubanItem, onClick: () -> Unit) {
    Column(
        Modifier.width(100.dp).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(100.dp, 140.dp).clip(RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (!item.cover.isNullOrBlank()) {
                AsyncImage(
                    model = item.cover,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Default.Movie, contentDescription = null,
                    tint = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(40.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(item.title, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Medium)
        if (!item.rate.isNullOrBlank()) {
            Text("Rating ${item.rate}", fontSize = 10.sp,
                color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun SourceRow(source: Source) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (source.kind == Source.Kind.HTML) Icons.Default.Public else Icons.Default.Code,
                contentDescription = null, tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(source.name, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text(source.url, fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            AssistChip(onClick = {}, label = { Text(source.kind.name) })
        }
    }
}

@Composable
private fun EmptySourceHint(onAdd: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.MovieFilter, contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(12.dp))
        Text("No video sources yet", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text("Tap the button below to add", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onAdd) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Add Source")
        }
    }
}
