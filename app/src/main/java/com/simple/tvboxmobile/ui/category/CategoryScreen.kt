package com.simple.tvboxmobile.ui.category

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.simple.tvbox.model.VideoItem
import com.simple.tvbox.source.VideoClientFactory
import com.simple.tvboxmobile.data.SourceAccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CategoryUiState(
    val loading: Boolean = false,
    val items: List<VideoItem> = emptyList(),
    val error: String? = null,
    val page: Int = 1,
    val hasMore: Boolean = true
)

class CategoryViewModel : ViewModel() {
    private val _state = MutableStateFlow(CategoryUiState())
    val state: StateFlow<CategoryUiState> = _state.asStateFlow()

    private var currentSiteKey: String = ""
    private var currentCatId: String = ""

    fun load(siteKey: String, categoryId: String, page: Int = 1) {
        if (page == 1) {
            currentSiteKey = siteKey
            currentCatId = categoryId
            _state.value = CategoryUiState(loading = true)
        } else {
            _state.value = _state.value.copy(loading = true)
        }
        viewModelScope.launch {
            try {
                val site = withContext(Dispatchers.IO) {
                    SourceAccess.repository().loadAllSites().find { it.key == siteKey }
                }
                if (site == null) {
                    _state.value = _state.value.copy(loading = false, error = "Source not found")
                    return@launch
                }
                val client = VideoClientFactory.create(site)
                if (!client.isSupported()) {
                    _state.value = _state.value.copy(loading = false, error = "Unsupported source")
                    return@launch
                }
                val items = withContext(Dispatchers.IO) {
                    client.fetchCategory(categoryId, page)
                }
                val merged = if (page == 1) items else _state.value.items + items
                _state.value = _state.value.copy(
                    loading = false,
                    items = merged,
                    page = page,
                    hasMore = items.size >= 20
                )
            } catch (t: Throwable) {
                _state.value = _state.value.copy(loading = false, error = t.message ?: "Unknown error")
            }
        }
    }

    fun loadMore() {
        if (!_state.value.loading && _state.value.hasMore) {
            load(currentSiteKey, currentCatId, _state.value.page + 1)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    siteKey: String,
    categoryId: String,
    categoryName: String,
    onItemClick: (VideoItem) -> Unit,
    onBack: () -> Unit,
    vm: CategoryViewModel = viewModel()
) {
    val state by vm.state.collectAsState()

    LaunchedEffect(siteKey, categoryId) {
        vm.load(siteKey, categoryId, 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(categoryName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (state.loading && state.items.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (state.error != null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Error: ${state.error}", color = MaterialTheme.colorScheme.error)
            }
        } else if (state.items.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No videos in this category")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.items) { item ->
                    VideoCard(item = item, onClick = { onItemClick(item) })
                }
                if (state.hasMore) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.loading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            } else {
                                TextButton(onClick = { vm.loadMore() }) { Text("Load More") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoCard(item: VideoItem, onClick: () -> Unit) {
    Column(
        Modifier.clickable(onClick = onClick).padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (!item.poster.isNullOrBlank()) {
                AsyncImage(
                    model = item.poster,
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
        if (!item.subTitle.isNullOrBlank()) {
            Text(item.subTitle, fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}
